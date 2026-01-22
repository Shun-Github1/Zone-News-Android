import SwiftUI

// MARK: - Card Order Management
enum AnalyticsCardType: String, CaseIterable, Identifiable, Codable {
    case sentiment = "sentiment"
    case timeline = "timeline"
    case mediaDistribution = "mediaDistribution"
    case subjectivity = "subjectivity"
    
    var id: String { rawValue }
    
    var localizedTitle: String {
        switch self {
        case .sentiment:
            return String(localized: "sentiment.title", table: "InfoPlist")
        case .timeline:
            return String(localized: "timeline.title", table: "InfoPlist")
        case .mediaDistribution:
            return String(localized: "publisher_distribution.title", table: "InfoPlist")
        case .subjectivity:
            return String(localized: "subjectivity.title", table: "InfoPlist")
        }
    }
}

class CardOrderManager: ObservableObject {
    static let shared = CardOrderManager()
    
    @Published var cardOrder: [AnalyticsCardType] {
        didSet {
            saveOrder()
        }
    }
    
    private let orderKey = "analyticsCardOrder"
    
    // Default/initial card order - this is the order that cards appear in when first installed
    static let defaultOrder: [AnalyticsCardType] = AnalyticsCardType.allCases
    
    init() {
        if let data = UserDefaults.standard.data(forKey: orderKey),
           let savedOrder = try? JSONDecoder().decode([AnalyticsCardType].self, from: data),
           savedOrder.count == AnalyticsCardType.allCases.count {
            self.cardOrder = savedOrder
        } else {
            self.cardOrder = Self.defaultOrder
        }
        
        // Listen for settings reset notification
        NotificationCenter.default.addObserver(
            forName: .settingsDidReset,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.resetToDefault()
        }
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    private func saveOrder() {
        if let data = try? JSONEncoder().encode(cardOrder) {
            UserDefaults.standard.set(data, forKey: orderKey)
        }
    }
    
    func move(from source: IndexSet, to destination: Int) {
        cardOrder.move(fromOffsets: source, toOffset: destination)
        // Haptic feedback
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.impactOccurred()
    }
    
    func resetToDefault() {
        print("🔄 Resetting card order to default: \(Self.defaultOrder.map { $0.rawValue })")
        cardOrder = Self.defaultOrder
    }
}

// MARK: - Drag Handle View
struct CardDragHandle: View {
    let isDragging: Bool
    
    var body: some View {
        Image(systemName: "line.3.horizontal")
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(isDragging ? Color(hex: "#239b98") : Color.infoSecondary)
            .frame(width: 24, height: 24)
            .contentShape(Rectangle())
            .scaleEffect(isDragging ? 1.1 : 1.0)
            .animation(.easeInOut(duration: 0.15), value: isDragging)
    }
}

// MARK: - Reorderable Card Header Component
struct ReorderableCardHeader: View {
    let title: String
    let isExpanded: Bool
    let isDragging: Bool
    let onToggleExpand: () -> Void
    let onInfoTapped: () -> Void
    var leadingPadding: CGFloat = 0
    
    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            if leadingPadding > 0 {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.textPrimary)
                    .padding(.leading, leadingPadding)
            } else {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.textPrimary)
            }
            
            Spacer()
            
            // Collapse button
            Button {
                onToggleExpand()
            } label: {
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                    .foregroundColor(Color.infoSecondary)
                    .font(.system(size: 14, weight: .medium))
            }
            .accessibilityLabel(isExpanded ? String(localized: "card.collapse", table: "InfoPlist") : String(localized: "card.expand", table: "InfoPlist"))
            
            // Drag handle - positioned after collapse button
            CardDragHandle(isDragging: isDragging)
                .accessibilityLabel(String(localized: "card.drag.label", defaultValue: "Drag to reorder", table: "InfoPlist"))
                .accessibilityHint(String(localized: "card.drag.hint", defaultValue: "Press and hold, then drag to change card order", table: "InfoPlist"))
            
            // Info button
            Button {
                onInfoTapped()
            } label: {
                Image(systemName: "info.circle")
                    .foregroundColor(Color.infoSecondary)
            }
        }
    }
}

// MARK: - Draggable Card Wrapper
struct DraggableCardWrapper<Content: View>: View {
    let cardType: AnalyticsCardType
    let content: Content
    @ObservedObject var orderManager: CardOrderManager
    @State private var isDragging = false
    @State private var dragOffset: CGSize = .zero
    @GestureState private var isLongPressing = false
    
    init(cardType: AnalyticsCardType, orderManager: CardOrderManager, @ViewBuilder content: () -> Content) {
        self.cardType = cardType
        self.orderManager = orderManager
        self.content = content()
    }
    
    var body: some View {
        content
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .preference(key: CardFramePreferenceKey.self, value: [cardType: geometry.frame(in: .named("cardContainer"))])
                }
            )
            .scaleEffect(isDragging ? 1.02 : 1.0)
            .shadow(color: isDragging ? .black.opacity(0.2) : .clear, radius: isDragging ? 8 : 0, x: 0, y: isDragging ? 4 : 0)
            .zIndex(isDragging ? 1 : 0)
            .offset(dragOffset)
            .animation(.interactiveSpring(response: 0.3, dampingFraction: 0.7), value: isDragging)
            .animation(.interactiveSpring(response: 0.3, dampingFraction: 0.7), value: dragOffset)
    }
}

// MARK: - Card Frame Preference Key
struct CardFramePreferenceKey: PreferenceKey {
    static var defaultValue: [AnalyticsCardType: CGRect] = [:]
    
    static func reduce(value: inout [AnalyticsCardType: CGRect], nextValue: () -> [AnalyticsCardType: CGRect]) {
        value.merge(nextValue()) { $1 }
    }
}

// MARK: - Reorderable Cards Container
struct ReorderableCardsContainer: View {
    let article: DetailedArticle
    @ObservedObject var cardOrderManager: CardOrderManager
    let showReportPatterns: Bool
    let isProUser: Bool
    let originalTitle: String? // Original title from first load (for timeline card)
    
    @State private var draggingCard: AnalyticsCardType?
    @State private var dragOffset: CGFloat = 0
    @State private var cardFrames: [AnalyticsCardType: CGRect] = [:]
    @State private var initialDragCardIndex: Int?
    @State private var lastHapticIndex: Int? // Track last haptic feedback position
    @State private var targetIndex: Int? // Current target index during drag
    
    // Filter cards based on settings
    private var visibleCards: [AnalyticsCardType] {
        cardOrderManager.cardOrder.filter { cardType in
            if cardType == .mediaDistribution && !showReportPatterns {
                return false
            }
            return true
        }
    }
    
    var body: some View {
        VStack(spacing: 20) {
            ForEach(visibleCards) { cardType in
                cardView(for: cardType)
                    .background(
                        GeometryReader { geo in
                            Color.clear.preference(
                                key: CardFramePreferenceKey.self,
                                value: [cardType: geo.frame(in: .named("reorderContainer"))]
                            )
                        }
                    )
                    .offset(y: offsetFor(cardType))
                    .zIndex(draggingCard == cardType ? 100 : 0)
                    .scaleEffect(draggingCard == cardType ? 1.03 : 1.0)
                    .shadow(
                        color: draggingCard == cardType ? .black.opacity(0.2) : .clear,
                        radius: draggingCard == cardType ? 12 : 0,
                        x: 0,
                        y: draggingCard == cardType ? 6 : 0
                    )
                    // Apply different animations based on state
                    .animation(
                        draggingCard == cardType
                            ? .interactiveSpring(response: 0.25, dampingFraction: 0.8) // Snappier for dragging card
                            : .spring(response: 0.35, dampingFraction: 0.85, blendDuration: 0.1), // Smooth for other cards
                        value: offsetFor(cardType)
                    )
                    .animation(
                        .spring(response: 0.3, dampingFraction: 0.75),
                        value: draggingCard
                    )
            }
        }
        .coordinateSpace(name: "reorderContainer")
        .onPreferenceChange(CardFramePreferenceKey.self) { frames in
            cardFrames = frames
        }
    }
    
    @ViewBuilder
    private func cardView(for cardType: AnalyticsCardType) -> some View {
        switch cardType {
        case .sentiment:
            SentimentMeterCard(
                sentiment: article.metrics.sentiment,
                isDragging: draggingCard == cardType,
                onDragChanged: { value in handleDrag(cardType: cardType, value: value) },
                onDragEnded: { handleDragEnd() }
            )
        case .timeline:
            TimelineCard(
                currentArticle: article,
                originalTitle: originalTitle,
                isDragging: draggingCard == cardType,
                onDragChanged: { value in handleDrag(cardType: cardType, value: value) },
                onDragEnded: { handleDragEnd() }
            )
        case .mediaDistribution:
            PublisherDistributionCard(
                coverage: article.coverage,
                isProUser: isProUser,
                isDragging: draggingCard == cardType,
                onDragChanged: { value in handleDrag(cardType: cardType, value: value) },
                onDragEnded: { handleDragEnd() }
            )
            .tutorialHighlight(id: "mediaDistributionCard")
            .id("mediaDistributionCard")
        case .subjectivity:
            SubjectivityScoreCard(
                subjectivity: article.metrics.subjectivity,
                isProUser: isProUser,
                isDragging: draggingCard == cardType,
                onDragChanged: { value in handleDrag(cardType: cardType, value: value) },
                onDragEnded: { handleDragEnd() }
            )
            .tutorialHighlight(id: "subjectivityScoreCard")
            .id("subjectivityScoreCard")
        }
    }
    
    private func offsetFor(_ cardType: AnalyticsCardType) -> CGFloat {
        guard let dragging = draggingCard else {
            return 0
        }
        
        // The dragging card follows the drag gesture directly
        if dragging == cardType {
            return dragOffset
        }
        
        // Calculate offset for non-dragging cards - real-time reordering
        guard let draggingIndex = visibleCards.firstIndex(of: dragging),
              let currentIndex = visibleCards.firstIndex(of: cardType),
              let initialIndex = initialDragCardIndex,
              let draggingFrame = cardFrames[dragging],
              let currentFrame = cardFrames[cardType] else {
            return 0
        }
        
        // Calculate the dragged card's current center position
        let draggedCenter = draggingFrame.midY + dragOffset
        let currentCenter = currentFrame.midY
        
        // Calculate card height and spacing for smooth transitions
        let cardHeight = currentFrame.height
        let spacing: CGFloat = 20
        let totalCardHeight = cardHeight + spacing
        
        // Use target index if available for more accurate positioning
        let effectiveTargetIndex = targetIndex ?? initialIndex
        
        // Determine if this card should move based on the dragged card's position
        // Use a threshold (half the card height) for smoother transitions
        let threshold = totalCardHeight * 0.5
        
        // Dragging down: cards between initial and target positions move up
        if dragOffset > 0 {
            if currentIndex > initialIndex && currentIndex <= effectiveTargetIndex {
                // Calculate smooth interpolation based on how far the dragged card has moved
                if draggedCenter > currentCenter - threshold {
                    let progress = min(1.0, max(0.0, (draggedCenter - (currentCenter - threshold)) / totalCardHeight))
                    return -totalCardHeight * progress
                }
            }
        }
        // Dragging up: cards between target and initial positions move down
        else if dragOffset < 0 {
            if currentIndex < initialIndex && currentIndex >= effectiveTargetIndex {
                // Calculate smooth interpolation based on how far the dragged card has moved
                if draggedCenter < currentCenter + threshold {
                    let progress = min(1.0, max(0.0, ((currentCenter + threshold) - draggedCenter) / totalCardHeight))
                    return totalCardHeight * progress
                }
            }
        }
        
        return 0
    }
    
    private func targetIndex(for cardType: AnalyticsCardType) -> Int {
        guard let currentIndex = visibleCards.firstIndex(of: cardType),
              let frame = cardFrames[cardType] else {
            return 0
        }
        
        // Calculate the dragged card's center position
        let draggedCenter = frame.midY + dragOffset
        
        // Find the target index based on the dragged card's center position
        var bestIndex = currentIndex
        var minDistance: CGFloat = .greatestFiniteMagnitude
        
        for (index, otherCard) in visibleCards.enumerated() {
            guard let otherFrame = cardFrames[otherCard], index != currentIndex else {
                continue
            }
            
            let otherCenter = otherFrame.midY
            let distance = abs(draggedCenter - otherCenter)
            
            // If dragging down, only consider cards below
            if dragOffset > 0 && index > currentIndex && draggedCenter > otherCenter {
                if distance < minDistance {
                    minDistance = distance
                    bestIndex = index
                }
            }
            // If dragging up, only consider cards above
            else if dragOffset < 0 && index < currentIndex && draggedCenter < otherCenter {
                if distance < minDistance {
                    minDistance = distance
                    bestIndex = index
                }
            }
        }
        
        return bestIndex
    }
    
    private func handleDrag(cardType: AnalyticsCardType, value: DragGesture.Value) {
        if draggingCard == nil {
            draggingCard = cardType
            initialDragCardIndex = visibleCards.firstIndex(of: cardType)
            lastHapticIndex = initialDragCardIndex
            targetIndex = initialDragCardIndex
            
            // Haptic feedback when starting drag - use light impact for subtle feedback
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
        }
        
        dragOffset = value.translation.height
        
        // Calculate current target index during drag
        let newTargetIndex = targetIndex(for: cardType)
        
        // Provide haptic feedback when crossing position thresholds
        if newTargetIndex != targetIndex, let lastIndex = lastHapticIndex {
            // Only trigger haptic if we've moved to a different position
            if newTargetIndex != lastIndex {
                let generator = UISelectionFeedbackGenerator()
                generator.selectionChanged()
                lastHapticIndex = newTargetIndex
            }
        }
        
        targetIndex = newTargetIndex
    }
    
    private func handleDragEnd() {
        guard let dragging = draggingCard,
              let fromIndex = cardOrderManager.cardOrder.firstIndex(of: dragging) else {
            resetDragState()
            return
        }
        
        // Calculate target position based on final drag offset and velocity
        let finalTargetIdx = calculateTargetIndex(for: dragging)
        
        // Only reorder if the position actually changed
        if finalTargetIdx != fromIndex {
            // Use haptic feedback for successful reorder
            let generator = UINotificationFeedbackGenerator()
            generator.notificationOccurred(.success)
            
            // Use smooth spring animation for reordering
            // Apple's recommended spring parameters for list reordering
            withAnimation(.spring(response: 0.4, dampingFraction: 0.8, blendDuration: 0.1)) {
                cardOrderManager.move(
                    from: IndexSet(integer: fromIndex),
                    to: finalTargetIdx > fromIndex ? finalTargetIdx + 1 : finalTargetIdx
                )
            }
        } else {
            // Light haptic feedback if no reorder occurred
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
        }
        
        resetDragState()
    }
    
    private func calculateTargetIndex(for cardType: AnalyticsCardType) -> Int {
        guard let currentIndex = visibleCards.firstIndex(of: cardType),
              let frame = cardFrames[cardType] else {
            return 0
        }
        
        // Use the target index calculated during drag, or fall back to calculation
        if let target = targetIndex, target != currentIndex {
            return target
        }
        
        // Fallback calculation based on final position
        let draggedCenter = frame.midY + dragOffset
        var bestIndex = currentIndex
        var minDistance: CGFloat = .greatestFiniteMagnitude
        
        for (index, otherCard) in visibleCards.enumerated() {
            guard let otherFrame = cardFrames[otherCard], index != currentIndex else {
                continue
            }
            
            let otherCenter = otherFrame.midY
            let distance = abs(draggedCenter - otherCenter)
            
            // Determine target based on which card's center is closest
            if dragOffset > 0 && index > currentIndex && draggedCenter > otherCenter {
                if distance < minDistance {
                    minDistance = distance
                    bestIndex = index
                }
            } else if dragOffset < 0 && index < currentIndex && draggedCenter < otherCenter {
                if distance < minDistance {
                    minDistance = distance
                    bestIndex = index
                }
            }
        }
        
        return bestIndex
    }
    
    private func resetDragState() {
        // Use smooth spring animation for reset
        withAnimation(.spring(response: 0.35, dampingFraction: 0.85, blendDuration: 0.1)) {
            draggingCard = nil
            dragOffset = 0
            initialDragCardIndex = nil
            targetIndex = nil
            lastHapticIndex = nil
        }
    }
}

// MARK: - Summary Settings Enums
enum DepthLevel: String, CaseIterable {
    case straightforward = "straightforward"
    case nuanced = "nuanced"
    
    var localizedString: String {
        switch self {
        case .straightforward:
            return String(localized: "summary.depth.straightforward", table: "InfoPlist")
        case .nuanced:
            return String(localized: "summary.depth.nuanced", table: "InfoPlist")
        }
    }
    
    var sfSymbolName: String {
        switch self {
        case .straightforward:
            return "arrow.right.circle"
        case .nuanced:
            return "waveform"
        }
    }
}

enum SummaryLanguage: String, CaseIterable {
    case english = "english"
    case traditionalChinese = "traditional_chinese"
    case simplifiedChinese = "simplified_chinese"
    
    var localizedString: String {
        switch self {
        case .english:
            return "English"
        case .traditionalChinese:
            return "繁體中文"
        case .simplifiedChinese:
            return "简体中文"
        }
    }
    
    /// Convert SummaryLanguage to Language enum for API requests
    var toLanguage: Language {
        switch self {
        case .english:
            return .englishUK
        case .traditionalChinese:
            return .traditionalChinese
        case .simplifiedChinese:
            return .simplifiedChinese
        }
    }
    
    /// Convert Language enum to SummaryLanguage
    static func fromLanguage(_ language: Language) -> SummaryLanguage {
        switch language {
        case .englishUK:
            return .english
        case .traditionalChinese:
            return .traditionalChinese
        case .simplifiedChinese:
            return .simplifiedChinese
        }
    }
}

// MARK: - Enhanced News Detail View
struct NewsDetailView: View {
    let articleID: String
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = NewsDetailViewModel()
    @State private var showingShareSheet = false
    @State private var showingFeedbackSheet = false
    @State private var feedbackText = ""
    @FocusState private var isFeedbackFocused: Bool
    @State private var showingInfo = false
    @EnvironmentObject private var tabCoordinator: TabCoordinator
    @EnvironmentObject private var appState: AppState
    @State private var shareActivityItems: [Any] = []
    @State private var showingComingSoonAlert = false
    
    // Summary settings state
    @State private var selectedDepthLevel: DepthLevel = .nuanced
    @State private var selectedLanguage: SummaryLanguage = .english
    
    // Enhanced gesture state for native-like swipe-to-go-back
    @State private var dragOffset: CGFloat = 0
    @State private var isDragging = false
    @State private var screenWidth: CGFloat = UIScreen.main.bounds.width
    @State private var gestureStartLocation: CGPoint = .zero
    @State private var isGestureActive = false // Track if gesture should be active
    @State private var scrollViewContentOffset: CGFloat = 0 // Track scroll position
    
    // Tutorial state
    @ObservedObject private var tutorialManager = TutorialManager.shared
    @ObservedObject private var cardOrderManager = CardOrderManager.shared
    @State private var isShowingTutorial: Bool = false
    @State private var isReorderingCards: Bool = false
    @State private var currentTutorialStep: Int = 0
    @State private var tutorialHighlightFrames: [String: CGRect] = [:]
    @State private var tutorialScrollProxy: ScrollViewProxy? = nil
    
    // Scroll position restoration
    @State private var savedScrollOffset: CGFloat = 0
    @State private var shouldRestoreScrollPosition: Bool = false
    @State private var hasNavigatedAway: Bool = false
    @State private var isViewVisible: Bool = false
    @State private var isRestoringScroll: Bool = false // Blocks offset updates during entire restoration process
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                mainContent
                    .onPreferenceChange(TutorialHighlightPreferenceKey.self) { frames in
                        tutorialHighlightFrames = frames
                    }
                
                // Tutorial Overlay
                if isShowingTutorial {
                    NewsDetailTutorialOverlay(
                        currentStep: $currentTutorialStep,
                        isShowing: $isShowingTutorial,
                        highlightFrames: tutorialHighlightFrames,
                        onComplete: {
                            tutorialManager.markNewsDetailTutorialCompleted()
                        },
                        onScrollToAnchor: { anchor in
                            withAnimation(.easeInOut(duration: 0.3)) {
                                let position: UnitPoint = anchor == "summaryParagraphs" ? UnitPoint(x: 0.5, y: 0.3) : .top
                                tutorialScrollProxy?.scrollTo(anchor, anchor: position)
                            }
                        }
                    )
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .navigationBarBackButtonHidden(true)
            .toolbar {
                toolbarContent
            }
            .toolbar(isShowingTutorial ? .hidden : .visible, for: .navigationBar)
            .toolbar(isShowingTutorial ? .hidden : .visible, for: .tabBar)
            .sheet(isPresented: $showingShareSheet) {
                shareSheet
            }
            .sheet(isPresented: $showingFeedbackSheet) {
                feedbackSheet
            }
            .alert(String(localized: "profile.menu.feedback.title", table: "InfoPlist"), isPresented: .constant(!viewModel.feedbackMessage.isEmpty)) {
                Button(String(localized: "button.ok", table: "InfoPlist")) {
                    viewModel.feedbackMessage = ""
                }
            } message: {
                Text(viewModel.feedbackMessage)
            }
            .alert(String(localized: "coming.soon.title", defaultValue: "Coming soon!", table: "InfoPlist"), isPresented: $showingComingSoonAlert) {
                Button(String(localized: "button.ok", table: "InfoPlist")) { }
            } message: {
                Text(String(localized: "coming.soon.message", defaultValue: "This feature is coming soon!", table: "InfoPlist"))
            }
            .onAppear {
                // Initialize selectedLanguage based on the actual language in use
                let currentLanguage = NetworkService.shared.getCurrentLanguage()
                selectedLanguage = SummaryLanguage.fromLanguage(currentLanguage)
                
                viewModel.loadArticleDetail(articleID: articleID)
                screenWidth = geometry.size.width
                tabCoordinator.updateTabBarState(.newsDetail)
            }
            .onChange(of: geometry.size.width) { oldValue, newValue in
                // Update screen width when view size changes (e.g., rotation, split view)
                screenWidth = newValue
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    withAnimation(.easeInOut(duration: 0.1)) {
                    }
                }
                
                // Show tutorial if needed (with delay to let content load)
                if tutorialManager.shouldShowNewsDetailTutorial {
                    // Expand all collapsible cards before showing tutorial
                    UserDefaults.standard.set(true, forKey: "sentimentCardExpanded")
                    UserDefaults.standard.set(true, forKey: "timelineCardExpanded")
                    UserDefaults.standard.set(true, forKey: "subjectivityCardExpanded")
                    UserDefaults.standard.set(true, forKey: "mediaDistributionCardExpanded")
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                        isShowingTutorial = true
                    }
                }
            }
            .onChange(of: selectedLanguage) { oldValue, newValue in
                // Reload article with new language when language setting changes
                guard oldValue != newValue else { return }
                viewModel.reloadArticleDetail(articleID: articleID, language: newValue.toLanguage)
            }
            .onReceive(NotificationCenter.default.publisher(for: .tutorialsReset)) { _ in
                // Reset tutorial step - tutorial will be triggered when page is loaded/visible
                currentTutorialStep = 0
                
                // Expand all collapsible cards when tutorial is reset
                if tutorialManager.shouldShowNewsDetailTutorial {
                    UserDefaults.standard.set(true, forKey: "sentimentCardExpanded")
                    UserDefaults.standard.set(true, forKey: "timelineCardExpanded")
                    UserDefaults.standard.set(true, forKey: "subjectivityCardExpanded")
                    UserDefaults.standard.set(true, forKey: "mediaDistributionCardExpanded")
                    
                    // Trigger tutorial if user is already on this page
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                        isShowingTutorial = true
                    }
                }
            }
            .onChange(of: isShowingTutorial) { _, newValue in
                // Hide/show tab bar at UIKit level during tutorial
                tabCoordinator.shouldHideTabBarForTutorial = newValue
            }
            .onDisappear {
                // Reset tutorial tab bar flag when leaving view
                tabCoordinator.shouldHideTabBarForTutorial = false
            }
            .onChange(of: tabCoordinator.shouldDismissCurrentNavigation) { oldValue, newValue in
                if newValue {
                    dismiss()
                }
            }
            // Enhanced native-like swipe gesture following Apple's best practices
            // - Only activates from left edge (20-30 points) following HIG guidelines
            // - Uses simultaneousGesture to work properly with ScrollView
            // - Respects scroll position to avoid conflicts
            // - Matches iOS system animation curves and thresholds
            .simultaneousGesture(
                DragGesture(minimumDistance: 0, coordinateSpace: .local)
                    .onChanged { value in
                        // Initialize gesture state on first touch
                        if !isDragging {
                            gestureStartLocation = value.startLocation
                            
                            // Apple HIG: Edge swipe should start within 20-30 points from left edge
                            // Also support full-screen swipe for iOS 26+ compatibility
                            let edgeThreshold: CGFloat = 30
                            let isFromLeftEdge = gestureStartLocation.x < edgeThreshold
                            
                            // Check if drag is primarily horizontal (not vertical scrolling)
                            let isHorizontalDrag = abs(value.translation.width) > abs(value.translation.height)
                            
                            // Only activate if:
                            // 1. Starting from left edge, OR
                            // 2. ScrollView is at top (allows full-screen swipe when scrolled to top)
                            // AND the drag is primarily horizontal (not vertical scrolling)
                            let canActivate = (isFromLeftEdge || scrollViewContentOffset <= 0) && 
                                            isHorizontalDrag && 
                                            value.translation.width > 0
                            
                            if canActivate {
                                isGestureActive = true
                                isDragging = true
                            } else {
                                isGestureActive = false
                            }
                        }
                        
                        // Only process if gesture is active and moving right
                        if isGestureActive && value.translation.width > 0 {
                            // Check if still primarily horizontal (user might have changed direction)
                            let isHorizontalDrag = abs(value.translation.width) > abs(value.translation.height)
                            
                            guard isHorizontalDrag else {
                                // If drag becomes vertical, deactivate gesture
                                isGestureActive = false
                                dragOffset = 0
                                return
                            }
                            
                            // Calculate sensitivity based on start location
                            // Edge swipes get full sensitivity, others get reduced
                            let edgeThreshold: CGFloat = 30
                            let isFromLeftEdge = gestureStartLocation.x < edgeThreshold
                            let sensitivity: CGFloat = isFromLeftEdge ? 1.0 : 0.5
                            
                            // Apply natural resistance curve (matches iOS behavior)
                            let rawOffset = value.translation.width * sensitivity
                            // Cap at 40% of screen width for natural feel
                            dragOffset = min(rawOffset, screenWidth * 0.4)
                        } else if !isGestureActive {
                            // Reset if gesture becomes inactive
                            dragOffset = 0
                        }
                    }
                    .onEnded { value in
                        let wasActive = isGestureActive
                        isDragging = false
                        isGestureActive = false
                        
                        // Only process completion if gesture was active
                        guard wasActive else {
                            dragOffset = 0
                            return
                        }
                        
                        // Apple-standard thresholds for gesture completion
                        // Distance threshold: 30% of screen width (matches iOS)
                        let distanceThreshold = screenWidth * 0.3
                        // Velocity threshold: 500 points/second (iOS standard)
                        let velocityThreshold: CGFloat = 500
                        // Minimum distance: 10% of screen width
                        let minimumDistance = screenWidth * 0.1
                        
                        // Completion logic following iOS standards:
                        // 1. Swipe past distance threshold, OR
                        // 2. Swipe past minimum distance with sufficient velocity
                        let shouldComplete = value.translation.width > distanceThreshold ||
                                           (value.translation.width > minimumDistance && 
                                            value.velocity.width > velocityThreshold)
                        
                        if shouldComplete {
                            // Use native dismiss() for system-consistent animation
                            dismiss()
                        } else {
                            // Reset with iOS-standard spring animation
                            // Matches system navigation animation parameters
                            withAnimation(.spring(response: 0.35, dampingFraction: 0.8, blendDuration: 0.1)) {
                                dragOffset = 0
                            }
                        }
                    }
            )
            // Apply visual feedback that matches iOS system behavior
            // Subtle offset multiplier for natural feel (matches system navigation)
            .offset(x: dragOffset)
            // Use system-matching animation curve
            .animation(.interactiveSpring(response: 0.3, dampingFraction: 0.85), value: dragOffset)
        }
    }
    
    @ViewBuilder
    private var mainContent: some View {
        Group {
            if viewModel.isLoading {
                loadingView
            } else if let article = viewModel.detailedArticle {
                articleContentView(article: article)
            } else if let errorMessage = viewModel.errorMessage {
                errorView(message: errorMessage)
            } else {
                loadingView
            }
        }
    }
    
    private var loadingView: some View {
        ProgressView(String(localized: "news.loading", table: "InfoPlist"))
            .progressViewStyle(CircularProgressViewStyle(tint: .gray))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.background)
    }
    
    private func articleContentView(article: DetailedArticle) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 0) {
                    headerImageView(article: article)
                    articleBodyView(article: article)
                }
                .padding(.bottom, 50) // Add bottom padding to prevent content from being covered by tab bar
            }
            .scrollDismissesKeyboard(.interactively)
            .background(Color.background)
            .background(
                ScrollViewAccessor(
                    savedOffset: savedScrollOffset,
                    shouldRestore: shouldRestoreScrollPosition,
                    onOffsetChange: { offset in
                        // Block all offset updates while restoration is pending or in progress
                        if !shouldRestoreScrollPosition && !isRestoringScroll {
                            savedScrollOffset = offset
                        }
                        // Update scroll position for gesture handling
                        scrollViewContentOffset = offset
                    },
                    onRestoreComplete: {
                        shouldRestoreScrollPosition = false
                        // Delay clearing the restoring flag to prevent immediate overwrites
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                            isRestoringScroll = false
                        }
                    }
                )
            )
            .onAppear {
                tutorialScrollProxy = proxy
                isViewVisible = true
                // When view appears after navigating away, restore scroll position
                // Use a small delay to ensure the view is fully laid out
                if hasNavigatedAway && savedScrollOffset > 50 {
                    // Block offset updates immediately to prevent overwriting
                    isRestoringScroll = true
                    // Try immediate restoration first
                    shouldRestoreScrollPosition = true
                    // Also try with a delay in case content isn't ready yet
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
                        if isViewVisible && savedScrollOffset > 50 {
                            shouldRestoreScrollPosition = true
                        }
                    }
                    hasNavigatedAway = false
                }
            }
            .onDisappear {
                isViewVisible = false
                // Track that we're navigating away (but only if we have scrolled meaningfully)
                // Use a threshold to avoid restoring for tiny scrolls
                if savedScrollOffset > 50 {
                    hasNavigatedAway = true
                }
            }
            .task(id: isViewVisible) {
                // When view becomes visible again, check if we need to restore
                if isViewVisible && hasNavigatedAway && savedScrollOffset > 50 {
                    // Block offset updates immediately
                    isRestoringScroll = true
                    try? await Task.sleep(nanoseconds: 200_000_000) // 0.2 seconds
                    if isViewVisible {
                        shouldRestoreScrollPosition = true
                        hasNavigatedAway = false
                    }
                }
            }
        }
    }
    
    private func headerImageView(article: DetailedArticle) -> some View {
        Group {
            if !article.pictureURL.isEmpty {
                ZStack(alignment: .bottomTrailing) {
                    AsyncImageWithPlaceholder(
                        url: URL(string: article.pictureURL),
                        aspectRatio: 16/9,
                        maxHeight: 200
                    )
                    
                    summarySettingsButton
                        .tutorialHighlight(id: "summarySettingsButton")
                        .padding(.trailing, 12)
                        .padding(.bottom, 12)
                }
            }
        }
    }
    
    private func articleBodyView(article: DetailedArticle) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            articleSummarySection(article: article)
                .id("summarySection")
            
            // Reorderable analytics cards
            ReorderableCardsContainer(
                article: article,
                cardOrderManager: cardOrderManager,
                showReportPatterns: appState.showReportPatterns,
                isProUser: appState.isProUser,
                originalTitle: viewModel.originalTitle
            )
            
            if !article.articles.isEmpty {
                PublisherArticlesListCard(articles: article.articles, viewModel: viewModel)
                    .tutorialHighlight(id: "firstArticleCard")
                    .id("firstArticleCard")
            }
        }
        .padding(20)
    }
    
    private func articleSummarySection(article: DetailedArticle) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            articleTitleAndDescription(article: article)
            
            VStack(alignment: .leading, spacing: 12) {
                // Synopsis Content
                if !article.description.synopsis.isEmpty {
                    Text(article.description.synopsis)
                        .font(.body)
                        .foregroundColor(.textSecondary)
                        .lineLimit(nil)
                }
                
                // Implications Section
                if !article.description.implications.isEmpty {
                    Text(article.description.implications)
                        .font(.body)
                        .foregroundColor(.textSecondary)
                        .lineLimit(nil)
                } else {
                    Text("")
                        .font(.body)
                        .foregroundColor(.textSecondary)
                        .lineLimit(nil)
                }
            }
            .tutorialHighlight(id: "summaryContent")
            .id("summaryParagraphs")
            
            // Generate Context Button, Attribution, and Feedback (moved below implications)
            generateContextButton(article: article)
            summaryAttributionRow
        }
    }
    
    private func articleTitleAndDescription(article: DetailedArticle) -> some View {
        HStack(spacing: 0) {
            Text(article.title)
                .font(.title2)
                .fontWeight(.semibold)
                .foregroundColor(.textPrimary)
                .lineLimit(nil)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
            Spacer(minLength: 0)
        }
        .accessibilityAddTraits(.isHeader)
    }
    
    private var summarySettingsButton: some View {
        Menu {
            // Depth Level Section
            Picker(String(localized: "summary.depth.title", table: "InfoPlist"), selection: $selectedDepthLevel) {
                ForEach(DepthLevel.allCases, id: \.self) { depthLevel in
                    HStack {
                        Image(systemName: depthLevel.sfSymbolName)
                            .foregroundColor(Color(uiColor: UIColor { traitCollection in
                                return traitCollection.userInterfaceStyle == .dark ? 
                                    UIColor.white : // White for dark mode
                                    UIColor.black   // Black for light mode
                            }))
                        Text(depthLevel.localizedString)
                    }
                    .tag(depthLevel)
                }
            }
            
            // Language Section
            Picker(String(localized: "summary.language.title", table: "InfoPlist"), selection: $selectedLanguage) {
                ForEach(SummaryLanguage.allCases, id: \.self) { language in
                    Text(language.localizedString)
                        .tag(language)
                }
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "gearshape")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.textPrimary)
            }
            .padding(.horizontal, 10)
            .frame(height: 35)
            .background(
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.buttonBackground)
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(Color.buttonBorder, lineWidth: 0.5)
                    )
            )
            .shadow(color: .black.opacity(0.3), radius: 4, x: 0, y: 2)
        }
        .fixedSize(horizontal: true, vertical: false)
        .accessibilityLabel(String(localized: "summary.settings.title", table: "InfoPlist"))
    }
    
    private func generateContextButton(article: DetailedArticle) -> some View {
        HStack {
            Spacer()
            Button(action: {
                showingComingSoonAlert = true
            }) {
                HStack(spacing: 6) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 14, weight: .medium))
                    Text(String(localized: "article.generate_context", table: "InfoPlist"))
                        .font(.system(size: 14, weight: .medium))
                }
                .foregroundColor(Color.backgroundSecondary)
                .frame(width: UIScreen.main.bounds.width * 0.67)
                .frame(height: 35)
                .background(Color.globalColor)
                .cornerRadius(8)
            }
            .accessibilityLabel(String(localized: "a11y.generate_context.label", table: "InfoPlist"))
            .accessibilityHint(String(localized: "a11y.generate_context.hint", table: "InfoPlist"))
            .tutorialHighlight(id: "generateContextButton")
            Spacer()
        }
    }
    
    private var summaryAttributionRow: some View {
        HStack {
            Text(String(localized: "article.summary.attribution", table: "InfoPlist"))
                .font(.caption)
                .foregroundColor(.gray.opacity(0.6))
            
            Spacer()
            
            Button(action: {
                showingFeedbackSheet = true
            }) {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.bubble.fill")
                        .font(.system(size: 12, weight: .medium))
                    Text(String(localized: "profile.menu.feedback.title", table: "InfoPlist"))
                        .font(.system(size: 13, weight: .medium))
                }
                .foregroundColor(.textPrimary)
                .padding(.horizontal, 12)
                .frame(height: 35)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.feedbackButtonBackground)
                        .overlay(
                            RoundedRectangle(cornerRadius: 8)
                                .stroke(Color.feedbackButtonBorder, lineWidth: 0.5)
                        )
                )
            }
            .scaleEffect(1.0)
            .animation(.easeInOut(duration: 0.1), value: showingFeedbackSheet)
            .accessibilityLabel(String(localized: "a11y.provide_feedback.label", table: "InfoPlist"))
            .accessibilityHint(String(localized: "a11y.provide_feedback.hint", table: "InfoPlist"))
        }
    }
    
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                                        .foregroundColor(.warning)
            
            Text(String(localized: "news.error_loading", table: "InfoPlist"))
                .font(.headline)
            
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button(String(localized: "button.retry", table: "InfoPlist")) {
                viewModel.loadArticleDetail(articleID: articleID)
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }
    
    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            Button(action: {
                dismiss()
            }) {
                Image(systemName: "chevron.left")
                    .font(.title2)
                    .fontWeight(.medium)
                    .foregroundColor(Color(uiColor: UIColor { traitCollection in
                        return traitCollection.userInterfaceStyle == .dark ? 
                            UIColor.white : // White for dark mode
                            UIColor.black   // Black for light mode
                    }))
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(PlainButtonStyle())
            .accessibilityLabel(String(localized: "a11y.back", table: "InfoPlist"))
            .accessibilityHint(String(localized: "a11y.back.hint", table: "InfoPlist"))
        }
        
        ToolbarItem(placement: .navigationBarTrailing) {
            HStack(spacing: 2) {
                saveButton
                shareButton
            }
        }
    }
    
    private var saveButton: some View {
        Button(action: {
            viewModel.toggleLiked()
        }) {
            Image(systemName: viewModel.isLiked ? "bookmark.fill" : "bookmark")
                .font(.title2)
                .foregroundColor(viewModel.isLiked ? Color(hex: "#239b98") : .primary)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(viewModel.isLiked ? String(localized: "a11y.unsave_article", table: "InfoPlist") : String(localized: "a11y.save_article", table: "InfoPlist"))
        .accessibilityHint(viewModel.isLiked ? String(localized: "a11y.unsave_article.hint", table: "InfoPlist") : String(localized: "a11y.save_article.hint", table: "InfoPlist"))
    }
    
    private var shareButton: some View {
        Button(action: {
            Task {
                await viewModel.trackShareAction()
                
                // Generate metadata before showing share sheet
                if let article = viewModel.detailedArticle {
                    if #available(iOS 17.0, *) {
                        let activityItem = ArticleActivityItem(detailedArticle: article)
                        if let itemSource = activityItem.getActivityItems().first as? ArticleActivityItemSource {
                            await itemSource.generateMetadata()
                        }
                        await MainActor.run {
                            shareActivityItems = activityItem.getActivityItems()
                            showingShareSheet = true
                        }
                    } else {
                        await MainActor.run {
                            shareActivityItems = [URL(string: article.shareURL)].compactMap { $0 }
                            showingShareSheet = true
                        }
                    }
                }
            }
        }) {
            Image(systemName: "square.and.arrow.up")
                .font(.title2)
                .foregroundColor(.primary)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(String(localized: "a11y.share_article", table: "InfoPlist"))
        .accessibilityHint(String(localized: "a11y.share_article.hint", table: "InfoPlist"))
    }
    
    private var shareSheet: some View {
        Group {
            if !shareActivityItems.isEmpty {
                EnhancedShareSheet(items: shareActivityItems)
            }
        }
    }
    
    private var feedbackSheet: some View {
        FeedbackSheetContent(
            feedbackText: $feedbackText,
            isFeedbackFocused: $isFeedbackFocused,
            onSubmit: { content in
                await viewModel.submitFeedback(content: content)
            },
            onSuccess: {
                feedbackText = ""
                showingFeedbackSheet = false
            }
        )
    }
}

// MARK: - Sentiment Meter Card
struct SentimentMeterCard: View {
    let sentiment: Double
    var isDragging: Bool = false
    var onDragChanged: ((DragGesture.Value) -> Void)?
    var onDragEnded: (() -> Void)?
    
    @State private var animatedSentiment: Double = 0.0
    @State private var hasAnimated: Bool = false
    @State private var displayValue: Double = 0.0
    @State private var showingInfo = false
    @State private var isVisibleInViewport: Bool = false
    @AppStorage("sentimentCardExpanded") private var isExpanded: Bool = true
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center) {
                Text(String(localized: "sentiment.title", table: "InfoPlist"))
                    .font(.headline)
                    .foregroundColor(.textPrimary)
                Spacer()
                
                Button {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        isExpanded.toggle()
                    }
                } label: {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(Color.infoSecondary)
                        .font(.system(size: 14, weight: .medium))
                }
                .accessibilityLabel(isExpanded ? String(localized: "card.collapse", table: "InfoPlist") : String(localized: "card.expand", table: "InfoPlist"))
                
                // Drag handle
                CardDragHandle(isDragging: isDragging)
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                onDragChanged?(value)
                            }
                            .onEnded { _ in
                                onDragEnded?()
                            }
                    )
                    .accessibilityLabel(String(localized: "card.drag.label", defaultValue: "Drag to reorder", table: "InfoPlist"))
                    .accessibilityHint(String(localized: "card.drag.hint", defaultValue: "Press and drag to change card order", table: "InfoPlist"))
                
                Button {
                    showingInfo = true
                } label: {
                    Image(systemName: "info.circle")
                        .foregroundColor(Color.infoSecondary)
                }
                .accessibilityLabel(String(localized: "sentiment.info.label", table: "InfoPlist"))
                .popover(isPresented: $showingInfo) {
                    EnhancedInfoBubble(maxWidth: 300, minWidth: 280) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(String(localized: "sentiment.info.description", table: "InfoPlist"))
                                .font(.subheadline)
                                .foregroundColor(.primary)
                                .lineLimit(nil)
                                .multilineTextAlignment(.leading)
                                .fixedSize(horizontal: false, vertical: true)
                            
                            HStack(spacing: 4) {
                                Text(String(localized: "sentiment.info.visit", table: "InfoPlist"))
                                    .foregroundColor(.secondary)
                                Link(String(localized: "sentiment.info.web", table: "InfoPlist"), destination: URL(string: "https://www.example.com")!)
                                Text(String(localized: "sentiment.info.details", table: "InfoPlist"))
                                    .foregroundColor(.secondary)
                            }
                            .font(.caption)
                            .lineLimit(nil)
                            .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            
            if isExpanded {
            ZStack {
                // Background semi-circle
                Path { path in
                    let center = CGPoint(x: 100, y: 100)
                    let radius: CGFloat = 80
                    path.addArc(center: center, radius: radius + 6, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                }
                .stroke(Color.gray.opacity(0.4), lineWidth: 2) // Darkened to match article feed grey
                
                Path { path in
                    let center = CGPoint(x: 100, y: 100)
                    let radius: CGFloat = 80
                    path.addArc(center: center, radius: radius - 6, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                }
                .stroke(Color.gray.opacity(0.4), lineWidth: 2) // Darkened to match article feed grey
                
                Path { path in
                    let start = CGPoint(x: 100 + 80 - 6, y: 100)
                    let end = CGPoint(x: 100 + 80 - 6 + 12, y: 100)
                    path.move(to: start)
                    path.addLine(to: end)
                }
                .stroke(Color.gray.opacity(0.4), lineWidth: 2) // Darkened to match article feed grey
                
                Path { path in
                    let start = CGPoint(x: 100 - 80 + 6, y: 100)
                    let end = CGPoint(x: 100 - 80 + 6 - 12, y: 100)
                    path.move(to: start)
                    path.addLine(to: end)
                }
                .stroke(Color.gray.opacity(0.4), lineWidth: 2) // Darkened to match article feed grey
                
                // Sentiment arc
                if animatedSentiment != 0 {
                    Path { path in
                        let center = CGPoint(x: 100, y: 100)
                        let radius: CGFloat = 80
                        let angle = abs(animatedSentiment) * 90 // Convert -1 to 1 range to 0 to 180 degrees
                        
                        if animatedSentiment < 0 {
                            // Anti-clockwise (negative sentiment)
                            path.addArc(center: center, radius: radius, startAngle: .degrees(270), endAngle: .degrees(270 - angle), clockwise: true) // reversed, because up-side down
                        } else {
                            // Clockwise (positive sentiment)
                            path.addArc(center: center, radius: radius, startAngle: .degrees(270), endAngle: .degrees(270 + angle), clockwise: false)
                        }
                    }
                    .stroke(animatedSentiment < 0 ? Color(hex: "#7F2538") : Color(hex: "#239b98"), lineWidth: 12)
                }
                
                // Labels
            Text("0")
                    .font(.caption)
                    .foregroundColor(Color.infoSecondary)
                    .position(x: 100, y: 0)
                
            // Left side labels
            VStack(spacing: 2) {
                Text("-1")
                    .font(.caption)
                    .foregroundColor(Color.infoSecondary)
                Text(String(localized: "sentiment.pessimistic", table: "InfoPlist"))
                    .font(.caption2)
                    .foregroundColor(Color.infoSecondary)
            }
            .position(x: 20, y: 118)
                
            // Right side labels
            VStack(spacing: 2) {
                Text("+1")
                    .font(.caption)
                    .foregroundColor(Color.infoSecondary)
                Text(String(localized: "sentiment.optimistic", table: "InfoPlist"))
                    .font(.caption2)
                    .foregroundColor(Color.infoSecondary)
            }
            .position(x: 180, y: 118)
                
                // Current value indicator
                let sentiment_val_string = (displayValue > 0 ? "+" : "") + String(format: "%.2f", displayValue)
                
                VStack {
                    Text(sentiment_val_string)
                        .font(.custom("BigDataField", size: 45))
                        .fontWeight(.medium)
                        .foregroundColor(displayValue < 0 ? Color(hex: "#7F2538") : (displayValue > 0 ? Color(hex: "#239b98") : Color.infoSecondary))
                }
                .position(x: 100, y: 85)
            }
            .frame(width: 200, height: 120)
            .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, isExpanded ? 24 : 16)
        .background(Color.newsDetailCardBackground)
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(localized: "sentiment.title", table: "InfoPlist"))
        .accessibilityValue(String(
            format: String(localized: "sentiment.accessibility_value_format", table: "InfoPlist"),
            String(format: "%.1f", sentiment)
        ))
        .background(
            GeometryReader { geometry in
                Color.clear
                    .onAppear {
                        // Check if card is initially visible in viewport
                        checkInitialVisibility(geometry: geometry)
                    }
                    .onChange(of: geometry.frame(in: .global)) { oldValue, newValue in
                        // Monitor scroll position changes
                        checkVisibility(geometry: geometry)
                    }
            }
        )
        .onChange(of: isVisibleInViewport) { oldValue, newValue in
            // Trigger animation when card becomes visible
            if newValue && !hasAnimated {
                startAnimation()
            }
        }
        .onChange(of: sentiment) { oldValue, newValue in
            // Reset animation state when sentiment changes
            if !hasAnimated && isVisibleInViewport {
                startAnimation()
            }
        }
    }
    
    private func startAnimation() {
        hasAnimated = true
        animatedSentiment = 0.0
        displayValue = 0.0
        
        // Animate both the arc and number with counting effect
        let totalSteps = 60 // Number of steps for counting
        let stepDuration = 0.8 / Double(totalSteps) // Duration per step (faster: 0.8 seconds)
        
        for step in 0...totalSteps {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(step) * stepDuration) {
                let progress = Double(step) / Double(totalSteps)
                displayValue = sentiment * progress
                animatedSentiment = sentiment * progress
            }
        }
    }
    
    private func checkInitialVisibility(geometry: GeometryProxy) {
        // Check if the card is initially visible in the viewport
        let frame = geometry.frame(in: .global)
        let screenHeight = UIScreen.main.bounds.height
        
        // Card is visible if it's within the screen bounds
        let isVisible = frame.minY < screenHeight && frame.maxY > 0
        
        DispatchQueue.main.async {
            self.isVisibleInViewport = isVisible
        }
    }
    
    private func checkVisibility(geometry: GeometryProxy) {
        // Check if the card is currently visible in the viewport during scrolling
        let frame = geometry.frame(in: .global)
        let screenHeight = UIScreen.main.bounds.height
        
        // Card is visible if it's within the screen bounds
        let isVisible = frame.minY < screenHeight && frame.maxY > 0
        
        DispatchQueue.main.async {
            self.isVisibleInViewport = isVisible
        }
    }
}

// MARK: - Subjectivity Score Card
struct SubjectivityScoreCard: View {
    let subjectivity: Double
    let isProUser: Bool
    var isDragging: Bool = false
    var onDragChanged: ((DragGesture.Value) -> Void)?
    var onDragEnded: (() -> Void)?
    
    @State private var showingInfo = false
    @State private var showingSubscription = false
    @AppStorage("subjectivityCardExpanded") private var isExpanded: Bool = true

    private var formattedValue: String {
        // Display to 2 significant figures, as required
        String(format: "%.2g", subjectivity)
    }

    private var statusTextLocalized: String {
        if subjectivity > 0.66 {
            return String(localized: "subjectivity.status.low", table: "InfoPlist")
        } else if subjectivity > 0.33 && subjectivity < 0.66 {
            return String(localized: "subjectivity.status.medium", table: "InfoPlist")
        } else if subjectivity < 0.33 {
            return String(localized: "subjectivity.status.high", table: "InfoPlist")
        } else {
            return String(localized: "subjectivity.status.medium", table: "InfoPlist")
        }
    }

    private var statusTextColor: Color {
        if subjectivity > 0.66 { return Color(hex: "#879693") }
        if subjectivity > 0.33 && subjectivity < 0.66 { return Color(hex: "#9AEDDD") }
        if subjectivity < 0.33 { return Color(hex: "#239b98") }
        return Color(hex: "#9AEDDD")
    }

    private var statusBackgroundColor: Color {
        if subjectivity > 0.66 { return Color(hex: "#2E3D3A") }
        if subjectivity > 0.33 && subjectivity < 0.66 { return Color(hex: "#3D776C") }
        if subjectivity < 0.33 { return Color(hex: "#9AEDDD") }
        return Color(hex: "#3D776C")
    }

    private var headerView: some View {
        HStack(alignment: .center) {
            Text(String(localized: "subjectivity.title", table: "InfoPlist"))
                .font(.headline)
                .foregroundColor(.textPrimary)
            Spacer()
            
            Button {
                withAnimation(.easeInOut(duration: 0.25)) {
                    isExpanded.toggle()
                }
            } label: {
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                    .foregroundColor(Color.infoSecondary)
                    .font(.system(size: 14, weight: .medium))
            }
            .accessibilityLabel(isExpanded ? String(localized: "card.collapse", table: "InfoPlist") : String(localized: "card.expand", table: "InfoPlist"))
            
            // Drag handle
            CardDragHandle(isDragging: isDragging)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            onDragChanged?(value)
                        }
                        .onEnded { _ in
                            onDragEnded?()
                        }
                )
                .accessibilityLabel(String(localized: "card.drag.label", defaultValue: "Drag to reorder", table: "InfoPlist"))
                .accessibilityHint(String(localized: "card.drag.hint", defaultValue: "Press and drag to change card order", table: "InfoPlist"))
            
            Button {
                showingInfo = true
            } label: {
                Image(systemName: "info.circle")
                    .foregroundColor(Color.infoSecondary)
            }
            .accessibilityLabel(String(localized: "subjectivity.info.label", table: "InfoPlist"))
            .popover(isPresented: $showingInfo) {
                EnhancedInfoBubble(maxWidth: 300, minWidth: 280) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(String(localized: "subjectivity.info.description", table: "InfoPlist"))
                            .font(.subheadline)
                            .foregroundColor(.primary)
                            .lineLimit(nil)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                        
                        HStack(spacing: 4) {
                            Text(String(localized: "subjectivity.info.visit", table: "InfoPlist"))
                                .foregroundColor(.secondary)
                            Link(String(localized: "subjectivity.info.web", table: "InfoPlist"), destination: URL(string: "https://www.example.com")!)
                            Text(String(localized: "subjectivity.info.details", table: "InfoPlist"))
                                .foregroundColor(.secondary)
                        }
                        .font(.caption)
                        .lineLimit(nil)
                        .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        // Remove blur from header - title and info button should be accessible for free users
    }

    private var mainContentView: some View {
        ZStack(alignment: .leading) {
            // Main data text with denominator in bottom-right
            ZStack(alignment: .bottomTrailing) {
                Text(formattedValue)
                    .font(.custom("BigDataField", size: 45))
                    .fontWeight(.medium)
                    .foregroundColor(.textPrimary)
                    .padding(.leading, 5)
                    .padding(.trailing, 12)
                Text("/1")
                    .font(.caption2)
                    .fontWeight(.light)
                    .foregroundColor(Color.infoSecondary)
                    .padding(.trailing, 2)
                    .padding(.bottom, 6)
            }
            .frame(minWidth: 80, alignment: .leading)

            // Status text bubble aligned with the right border of the information icon
            HStack {
                Spacer()
                
                // Status text bubble with fixed size for consistency across devices
                Text(statusTextLocalized)
                    .font(.system(size: 17, weight: .semibold)) // Fixed size instead of .body
                    .foregroundColor(statusTextColor)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(statusBackgroundColor)
                    )
                    .fixedSize(horizontal: true, vertical: false)
            }
        }
        .blur(radius: isProUser ? 0 : 8) // Blur main content for free users with stronger intensity
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header - always accessible (no blur, no lock overlay)
            headerView
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, isExpanded ? 12 : 16)
            
            if isExpanded {
            // Content area - locked for free users
            ZStack {
                VStack(alignment: .leading, spacing: 12) {
                    mainContentView
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
                .background(Color.newsDetailCardBackground)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.gray.opacity(isProUser ? 0.0 : 0.2), lineWidth: 1)
                )
                
                // Lock overlay for free users - only covers the content area
                Group {
                    if !isProUser {
                        ZStack {
                            // Blurred background
                            Color.black.opacity(0.3)
                                .blur(radius: 2)
                            
                            // Lock icon
                            VStack(spacing: 8) {
                                Image(systemName: "lock.fill")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                
                            }
                        }
                        .cornerRadius(8)
                    }
                }
            }
            .onTapGesture {
                // For non-pro users, show subscription when tapping the content area
                if !isProUser {
                    showingSubscription = true
                }
            }
            }
        }
        .background(Color.newsDetailCardBackground)
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
        .sheet(isPresented: $showingSubscription) {
            SubscriptionView()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(String(localized: "subjectivity.title", table: "InfoPlist"))
        .accessibilityValue(String(
            format: String(localized: "subjectivity.accessibility_value_format", table: "InfoPlist"),
            formattedValue
        ))
    }
}

// MARK: - Publisher Distribution Card
struct PublisherDistributionCard: View {
    let coverage: DetailedArticleCoverage
    let isProUser: Bool
    var isDragging: Bool = false
    var onDragChanged: ((DragGesture.Value) -> Void)?
    var onDragEnded: (() -> Void)?
    
    @State private var showingInfo = false
    @State private var showingSubscription = false
    @AppStorage("mediaDistributionCardExpanded") private var isExpanded: Bool = true
    
    private var headerView: some View {
        HStack(alignment: .center) {
            Text(String(localized: "publisher_distribution.title", table: "InfoPlist"))
                .font(.headline)
                .foregroundColor(.textPrimary)
            Spacer()
            
            Button {
                withAnimation(.easeInOut(duration: 0.25)) {
                    isExpanded.toggle()
                }
            } label: {
                Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                    .foregroundColor(Color.infoSecondary)
                    .font(.system(size: 14, weight: .medium))
            }
            .accessibilityLabel(isExpanded ? String(localized: "card.collapse", table: "InfoPlist") : String(localized: "card.expand", table: "InfoPlist"))
            
            // Drag handle
            CardDragHandle(isDragging: isDragging)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            onDragChanged?(value)
                        }
                        .onEnded { _ in
                            onDragEnded?()
                        }
                )
                .accessibilityLabel(String(localized: "card.drag.label", defaultValue: "Drag to reorder", table: "InfoPlist"))
                .accessibilityHint(String(localized: "card.drag.hint", defaultValue: "Press and drag to change card order", table: "InfoPlist"))
            
            Button {
                showingInfo = true
            } label: {
                Image(systemName: "info.circle")
                    .foregroundColor(Color.infoSecondary)
            }
            .accessibilityLabel(String(localized: "publisher_distribution.info.label", table: "InfoPlist"))
            .popover(isPresented: $showingInfo) {
                EnhancedInfoBubble(maxWidth: 300, minWidth: 280) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(String(localized: "publisher_distribution.info.description", table: "InfoPlist"))
                            .font(.subheadline)
                            .foregroundColor(.primary)
                            .lineLimit(nil)
                            .multilineTextAlignment(.leading)
                            .fixedSize(horizontal: false, vertical: true)
                        
                        HStack(spacing: 4) {
                            Text(String(localized: "sentiment.info.visit", table: "InfoPlist"))
                                .foregroundColor(.secondary)
                            Link(String(localized: "sentiment.info.web", table: "InfoPlist"), destination: URL(string: "https://www.example.com")!)
                            Text(String(localized: "sentiment.info.details", table: "InfoPlist"))
                                .foregroundColor(.secondary)
                        }
                        .font(.caption)
                        .lineLimit(nil)
                        .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        // Remove blur from header - title and info button should be accessible for free users
    }

    private var mainContentView: some View {
        GeometryReader { geometry in
            let width = geometry.size.width
            let height: CGFloat = 120
            let separationX = width * coverage.percentage.centric
            
            ZStack {
                // Background rectangles
                HStack(spacing: 0) {
                    Rectangle()
                        .fill(Color.distributionLight)
                        .frame(width: separationX)
                    
                    Rectangle()
                        .fill(Color.distributionDark)
                        .frame(width: width - separationX)
                }
                .frame(height: height)
                .cornerRadius(8)
                
                // Separation line
                    Rectangle()
                        .fill(Color(UIColor.separator))
                    .frame(width: 2, height: height)
                    .position(x: separationX, y: height / 2)
                
                // Centric publisher icons (left side)
                ForEach(Array(coverage.icons.centric.enumerated()), id: \.offset) { index, icon in
                    PublisherIconView(icon: icon, containerWidth: separationX, containerHeight: height, isProgressive: false)
                }
                
                // Progressive publisher icons (right side)
                ForEach(Array(coverage.icons.progressive.enumerated()), id: \.offset) { index, icon in
                    PublisherIconView(icon: icon, containerWidth: width - separationX, containerHeight: height, isProgressive: true, offsetX: separationX)
                }
                
            }
        }
        .frame(height: 120)
        .blur(radius: isProUser ? 0 : 8) // Blur entire content for free users with stronger intensity
    }

    private var legendView: some View {
        HStack {
            HStack(spacing: 4) {
                Rectangle()
                    .fill(Color.distributionLight)
                    .frame(width: 12, height: 12)
                    .cornerRadius(2)
                Text(String(localized: "publisher_distribution.centric", table: "InfoPlist"))
                    .font(.caption)
                    .foregroundColor(Color.infoSecondary)
            }
            
            Spacer()
            
            HStack(spacing: 4) {
                Rectangle()
                    .fill(Color.distributionDark)
                    .frame(width: 12, height: 12)
                    .cornerRadius(2)
                Text(String(localized: "publisher_distribution.progressive", table: "InfoPlist"))
                    .font(.caption)
                    .foregroundColor(Color.infoSecondary)
            }
        }
        .blur(radius: isProUser ? 0 : 8) // Blur legend for free users with stronger intensity
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header - always accessible (no blur, no lock overlay)
            headerView
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, isExpanded ? 12 : 16)
            
            if isExpanded {
            // Content area - locked for free users
            ZStack {
                VStack(alignment: .leading, spacing: 12) {
                    mainContentView
                    legendView
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
                .background(Color.newsDetailCardBackground)
                .cornerRadius(8)
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.gray.opacity(isProUser ? 0.0 : 0.2), lineWidth: 1)
                )
                
                // Lock overlay for free users - only covers the content area
                Group {
                    if !isProUser {
                        ZStack {
                            // Blurred background
                            Color.black.opacity(0.3)
                                .blur(radius: 2)
                            
                            // Lock icon
                            VStack(spacing: 8) {
                                Image(systemName: "lock.fill")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                
                            }
                        }
                        .cornerRadius(8)
                    }
                }
            }
            .onTapGesture {
                // For non-pro users, show subscription when tapping the content area
                if !isProUser {
                    showingSubscription = true
                }
            }
            }
        }
        .background(Color.newsDetailCardBackground)
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
        .sheet(isPresented: $showingSubscription) {
            SubscriptionView()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(String(localized: "publisher_distribution.title", table: "InfoPlist"))
        .accessibilityValue(String(
            format: String(localized: "publisher_distribution.accessibility_value_format", table: "InfoPlist"),
            Int(coverage.percentage.centric * 100),
            Int(coverage.percentage.progressive * 100)
        ))
    }
}

// MARK: - Publisher Icon View
struct PublisherIconView: View {
    let icon: CoverageIcon
    let containerWidth: CGFloat
    let containerHeight: CGFloat
    let isProgressive: Bool
    var offsetX: CGFloat = 0
    
    var body: some View {
        AsyncImage(url: URL(string: icon.logo)) { image in
            image
                .resizable()
                .aspectRatio(contentMode: .fit)
        } placeholder: {
            Circle()
                .fill(Color.backgroundTertiary)
                .overlay(
                    Image(systemName: "building.2")
                        .font(.caption)
                        .foregroundColor(.textTertiary)
                )
        }
        .frame(width: iconSize, height: iconSize)
        .clipShape(Circle())
        .position(x: iconXPosition, y: iconYPosition)
    }
    
    private var iconSize: CGFloat {
        // Scale icon size based on the size attribute (0.0 to 1.0)
        let minSize: CGFloat = 16
        let maxSize: CGFloat = 50
        return minSize + (maxSize - minSize) * icon.size
    }
    
    private var iconXPosition: CGFloat {
        var min_x = iconSize/2
        var max_x = containerWidth - iconSize/2
        if isProgressive {
            // For progressive: 0.0 is separation line, 1.0 is right edge
            min_x += offsetX
            max_x += offsetX
        } else {
            // For centric: 0.0 is left edge, 1.0 is separation line
        }
        
        return min_x + (max_x - min_x) * icon.rx
    }
    
    private var iconYPosition: CGFloat {
        // 0.0 is bottom, 1.0 is top
        let min_y = iconSize/2
        let max_y = containerHeight - iconSize/2
        return min_y + (max_y - min_y) * icon.ry
    }
}

// MARK: - Enhanced Information Popover with Dynamic Sizing
struct VariableInfoPopover<Content: View>: View {
    let content: Content
    let maxWidth: CGFloat
    let minWidth: CGFloat
    
    init(maxWidth: CGFloat = 300, minWidth: CGFloat = 280, @ViewBuilder content: () -> Content) {
        self.maxWidth = maxWidth
        self.minWidth = minWidth
        self.content = content()
    }
    
    var body: some View {
        content
            .frame(minWidth: minWidth, maxWidth: maxWidth)
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .presentationCompactAdaptation(.popover)
    }
}

// MARK: - Dynamic Text Size Calculator
struct TextSizeCalculator: View {
    let text: String
    let font: Font
    let maxWidth: CGFloat
    @Binding var calculatedHeight: CGFloat
    
    var body: some View {
        Text(text)
            .font(font)
            .lineLimit(nil)
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: maxWidth)
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .onAppear {
                            calculatedHeight = geometry.size.height
                        }
                        .onChange(of: geometry.size.height) { oldValue, newValue in
                            calculatedHeight = newValue
                        }
                }
            )
    }
}

// MARK: - Enhanced Information Bubble with Proper Height Calculation
struct EnhancedInfoBubble<Content: View>: View {
    let content: Content
    let maxWidth: CGFloat
    let minWidth: CGFloat
    @State private var contentHeight: CGFloat = 0
    
    init(maxWidth: CGFloat = 300, minWidth: CGFloat = 280, @ViewBuilder content: () -> Content) {
        self.maxWidth = maxWidth
        self.minWidth = minWidth
        self.content = content()
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            content
                .background(
                    GeometryReader { geometry in
                        Color.clear
                            .onAppear {
                                contentHeight = geometry.size.height
                            }
                            .onChange(of: geometry.size.height) { oldValue, newValue in
                                contentHeight = newValue
                            }
                    }
                )
        }
        .frame(minWidth: minWidth, maxWidth: maxWidth, minHeight: max(contentHeight + 32, 100))
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .presentationCompactAdaptation(.popover)
    }
}

// MARK: - Share Sheet
struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    
    func makeUIViewController(context: Context) -> UIActivityViewController {
        let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
        return controller
    }
    
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Publisher Articles List Card
struct PublisherArticlesListCard: View {
    let articles: [PublisherArticle]
    @ObservedObject var viewModel: NewsDetailViewModel
    @EnvironmentObject private var appState: AppState
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(String(localized: "publisher_articles.title", table: "InfoPlist"))
                    .font(.headline)
                    .foregroundColor(.textPrimary)
                
                Spacer()
                
                // Sort buttons - separate for method and order
                HStack(spacing: 8) {
                    // Sort Method Button
                    Menu {
                        Picker(String(localized: "sort.dialog.title", table: "InfoPlist"), selection: $viewModel.currentSortOption) {
                            ForEach(ArticleSortOption.allCases, id: \.self) { option in
                                HStack {
                                    Image(systemName: option.sfSymbolName)
                                        .foregroundColor(.primary)
                                    Text(option.localizedString)
                                        .lineLimit(1)
                                        .truncationMode(.tail)
                                        .minimumScaleFactor(0.8)
                                    Spacer()
                                    if viewModel.currentSortOption == option {
                                        Image(systemName: "checkmark")
                                            .foregroundColor(.accentColor)
                                    }
                                }
                                .tag(option)
                            }
                        }
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: viewModel.currentSortOption.sfSymbolName)
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.textPrimary)
                            
                            Text(viewModel.currentSortOption.localizedString)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.textPrimary)
                                .lineLimit(1)
                                .truncationMode(.tail)
                        }
                        .padding(.horizontal, 10)
                        .frame(height: 35)
                        .background(
                            RoundedRectangle(cornerRadius: 6)
                                .fill(Color.buttonBackground)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 6)
                                        .stroke(Color.buttonBorder, lineWidth: 0.5)
                                )
                        )
                        .fixedSize(horizontal: true, vertical: false)
                    }
                    .accessibilityLabel(String(localized: "sort.method.accessibility", table: "InfoPlist"))
                    .accessibilityHint(String(localized: "a11y.sort.method.hint", table: "InfoPlist"))
                    
                    // Sort Order Button
                    Button(action: {
                        viewModel.toggleSortDirection()
                    }) {
                        HStack(spacing: 3) {
                            // Up arrow
                            Image(systemName: "arrow.up")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundColor(viewModel.isSortAscending ? .textPrimary : .textSecondary)
                            
                            // Down arrow
                            Image(systemName: "arrow.down")
                                .font(.system(size: 10, weight: .semibold))
                                .foregroundColor(!viewModel.isSortAscending ? .textPrimary : .textSecondary)
                        }
                        .animation(.easeInOut(duration: 0.2), value: viewModel.isSortAscending)
                        .padding(.horizontal, 10)
                        .frame(height: 35)
                        .background(
                            RoundedRectangle(cornerRadius: 6)
                                .fill(Color.buttonBackground)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 6)
                                        .stroke(Color.buttonBorder, lineWidth: 0.5)
                                )
                        )
                    }
                    .accessibilityLabel(String(localized: "sort.order.accessibility", table: "InfoPlist"))
                    .accessibilityHint(String(localized: "a11y.sort.order.hint", table: "InfoPlist"))
                }
            }
            
            VStack(spacing: 0) {
                ForEach(Array(viewModel.sortedArticles.enumerated()), id: \.offset) { index, publisherArticle in
                    PublisherArticleRowView(article: publisherArticle, showStanceTags: appState.showReportPatterns, isProUser: appState.isProUser)
                    if index < viewModel.sortedArticles.count - 1 {
                        Divider()
                    }
                }
            }
        }
        .padding(16)
        .background(Color.newsDetailCardBackground)
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.gray.opacity(0.2), lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(String(localized: "publisher_articles.title", table: "InfoPlist"))
    }
}

// MARK: - Publisher Article Row View
struct PublisherArticleRowView: View {
    let article: PublisherArticle
    let showStanceTags: Bool
    let isProUser: Bool
    @EnvironmentObject private var personalizationSettings: PersonalizationSettings
    @State private var showingMediaOutletPopup = false
    
    private var stanceBackgroundColor: Color {
        let tag = article.publisherStance.tag.lowercased()
        if tag == "p" { // Progressive
            return Color(uiColor: UIColor { traitCollection in
                return traitCollection.userInterfaceStyle == .dark ? 
                    UIColor(red: 0.3, green: 0.3, blue: 0.3, alpha: 1.0) : // Dark grey background for dark mode
                    UIColor(red: 0.56, green: 0.56, blue: 0.58, alpha: 1.0)   // Dark grey background for light mode
            })
        }
        if tag == "c" { // Conservative/Centric
            return Color(uiColor: UIColor { traitCollection in
                return traitCollection.userInterfaceStyle == .dark ? 
                    UIColor(red: 0.4, green: 0.4, blue: 0.4, alpha: 1.0) : // Light grey background for dark mode
                    UIColor(red: 0.78, green: 0.78, blue: 0.8, alpha: 1.0)   // Light grey background for light mode
            })
        }
        return Color(uiColor: UIColor { traitCollection in
            return traitCollection.userInterfaceStyle == .dark ? 
                UIColor(red: 0.55, green: 0.58, blue: 0.63, alpha: 1.0) : // Default grey background for dark mode
                UIColor(red: 0.35, green: 0.38, blue: 0.43, alpha: 1.0)   // Default grey background for light mode
        })
    }
    
    private var stanceTextColor: Color {
        let tag = article.publisherStance.tag.lowercased()
        if tag == "p" { // Progressive
            return Color(uiColor: UIColor { traitCollection in
                return traitCollection.userInterfaceStyle == .dark ? 
                    UIColor(red: 0.8, green: 0.8, blue: 0.8, alpha: 1.0) : // Light grey text for dark mode
                    UIColor(red: 0.9, green: 0.9, blue: 0.9, alpha: 1.0)   // Light grey text for light mode
            })
        }
        if tag == "c" { // Conservative/Centric
            return Color(uiColor: UIColor { traitCollection in
                return traitCollection.userInterfaceStyle == .dark ? 
                    UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1.0) : // Dark grey text for dark mode
                    UIColor(red: 0.2, green: 0.2, blue: 0.2, alpha: 1.0)   // Dark grey text for light mode
            })
        }
        return Color(uiColor: UIColor { traitCollection in
            return traitCollection.userInterfaceStyle == .dark ? 
                UIColor(red: 0.9, green: 0.9, blue: 0.9, alpha: 1.0) : // Default light text for dark mode
                UIColor(red: 0.1, green: 0.1, blue: 0.1, alpha: 1.0)   // Default dark text for light mode
        })
    }
    
    var body: some View {
        Group {
            if let url = URL(string: article.articleURL), !article.articleURL.isEmpty {
                if personalizationSettings.articleOpeningMethod == .external {
                    Button(action: {
                        UIApplication.shared.open(url)
                    }) {
                        rowContent
                    }
                    .buttonStyle(.plain)
                } else {
                    NavigationLink(destination: ArticleWebView(url: url, mediaName: article.publisherName, mediaIcon: article.publisherIcon)) {
                        rowContent
                    }
                    .buttonStyle(.plain)
                }
            } else {
                rowContent
                    .opacity(0.6)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(article.publisherName), \(article.title)")
        .sheet(isPresented: $showingMediaOutletPopup) {
            MediaOutletPopupView(
                publisherID: article.publisherID,
                publisherName: article.publisherName,
                publisherIcon: article.publisherIcon
            )
        }
    }
    
    private var rowContent: some View {
        HStack(alignment: .top, spacing: 12) {
            Button(action: {
                showingMediaOutletPopup = true
            }) {
                AsyncImage(url: URL(string: article.publisherIcon)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Circle()
                        .fill(Color(uiColor: UIColor { traitCollection in
                            return traitCollection.userInterfaceStyle == .dark ? 
                                UIColor(red: 0.55, green: 0.58, blue: 0.63, alpha: 0.3) : // Darker grey for dark mode
                                UIColor(red: 0.35, green: 0.38, blue: 0.43, alpha: 0.3)   // Darker grey for light mode
                        }))
                        .overlay(
                            Image(systemName: "newspaper")
                                .font(.caption)
                                .foregroundColor(Color(uiColor: UIColor { traitCollection in
                                    return traitCollection.userInterfaceStyle == .dark ? 
                                        UIColor(red: 0.55, green: 0.58, blue: 0.63, alpha: 1.0) : // Darker grey for dark mode
                                        UIColor(red: 0.35, green: 0.38, blue: 0.43, alpha: 1.0)   // Darker grey for light mode
                                }))
                        )
                }
                .frame(width: 40, height: 40)
                .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .frame(width: 44, height: 44) // Minimum tap target size per Apple HIG
            .contentShape(Circle()) // Ensure the entire circular area is tappable
            .accessibilityLabel(String(localized: "media_outlet.popup.accessibility", defaultValue: "Media outlet information for \(article.publisherName)", table: "InfoPlist"))
            
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(article.publisherName.isEmpty ? String(localized: "publisher.unknown", table: "InfoPlist") : article.publisherName)
                        .font(.subheadline)
                        .foregroundColor(Color.infoSecondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    
                    if showStanceTags && !article.publisherStance.displayName.isEmpty {
                        ZStack(alignment: .center) {
                            Text(article.publisherStance.displayName)
                                .font(.caption)
                                .fontWeight(.semibold)
                                .foregroundColor(stanceTextColor)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(stanceBackgroundColor)
                                .cornerRadius(4)
                                .blur(radius: isProUser ? 0 : 8) // Blur for free users with stronger intensity
                            
                            if !isProUser {
                                Image(systemName: "lock.fill")
                                    .font(.caption2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.white)
                            }
                        }
                    }
                }
                
                Text(article.title)
                    .font(.body)
                    .foregroundColor(.textPrimary)
                    .lineLimit(nil)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.footnote)
                .foregroundColor(Color.infoSecondary)
                .padding(.top, 2)
        }
        .contentShape(Rectangle())
        .padding(.vertical, 10)
    }
}

// MARK: - Timeline Card
struct TimelineCard: View {
    let currentArticle: DetailedArticle
    let originalTitle: String? // Original title from first load (doesn't change with language)
    var isDragging: Bool = false
    var onDragChanged: ((DragGesture.Value) -> Void)?
    var onDragEnded: (() -> Void)?
    
    @State private var showingInfo = false
    @AppStorage("timelineCardExpanded") private var isExpanded: Bool = true
    
    // Mock timeline data - 3 articles before, current, 3 after
    private var timelineArticles: [TimelineArticle] {
        let mockArticles = [
            TimelineArticle(
                id: "timeline-1",
                title: String(localized: "timeline.mock.article1.title", table: "InfoPlist"),
                date: "2024-01-10 09:00:00",
                articleID: "timeline-1"
            ),
            TimelineArticle(
                id: "timeline-2", 
                title: String(localized: "timeline.mock.article2.title", table: "InfoPlist"),
                date: "2024-01-12 14:30:00",
                articleID: "timeline-2"
            ),
            TimelineArticle(
                id: "timeline-3",
                title: String(localized: "timeline.mock.article3.title", table: "InfoPlist"),
                date: "2024-01-14 11:15:00", 
                articleID: "timeline-3"
            ),
            TimelineArticle(
                id: currentArticle.articleID,
                title: originalTitle ?? currentArticle.title, // Use original title if available, otherwise fall back to current
                date: currentArticle.date,
                articleID: currentArticle.articleID,
                isCurrent: true
            ),
            TimelineArticle(
                id: "timeline-5",
                title: String(localized: "timeline.mock.article5.title", table: "InfoPlist"),
                date: "2024-01-16 16:45:00",
                articleID: "timeline-5"
            ),
            TimelineArticle(
                id: "timeline-6",
                title: String(localized: "timeline.mock.article6.title", table: "InfoPlist"),
                date: "2024-01-18 10:20:00",
                articleID: "timeline-6"
            ),
            TimelineArticle(
                id: "timeline-7",
                title: String(localized: "timeline.mock.article7.title", table: "InfoPlist"),
                date: "2024-01-20 13:00:00",
                articleID: "timeline-7"
            )
        ]
        return mockArticles
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header aligned with other cards
            HStack(alignment: .center, spacing: 8) {
                Text(String(localized: "timeline.title", table: "InfoPlist"))
                    .font(.headline)
                    .foregroundColor(.textPrimary)
                
                Spacer()
                
                Button {
                    withAnimation(.easeInOut(duration: 0.25)) {
                        isExpanded.toggle()
                    }
                } label: {
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(Color.infoSecondary)
                        .font(.system(size: 14, weight: .medium))
                }
                .accessibilityLabel(isExpanded ? String(localized: "card.collapse", table: "InfoPlist") : String(localized: "card.expand", table: "InfoPlist"))
                
                // Drag handle
                CardDragHandle(isDragging: isDragging)
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                onDragChanged?(value)
                            }
                            .onEnded { _ in
                                onDragEnded?()
                            }
                    )
                    .accessibilityLabel(String(localized: "card.drag.label", defaultValue: "Drag to reorder", table: "InfoPlist"))
                    .accessibilityHint(String(localized: "card.drag.hint", defaultValue: "Press and drag to change card order", table: "InfoPlist"))
                
                Button {
                    showingInfo = true
                } label: {
                    Image(systemName: "info.circle")
                        .foregroundColor(Color.infoSecondary)
                }
                .accessibilityLabel(String(localized: "timeline.info.label", table: "InfoPlist"))
                .popover(isPresented: $showingInfo) {
                    EnhancedInfoBubble(maxWidth: 300, minWidth: 280) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(String(localized: "timeline.info.description", table: "InfoPlist"))
                                .font(.subheadline)
                                .foregroundColor(.primary)
                                .lineLimit(nil)
                                .multilineTextAlignment(.leading)
                                .fixedSize(horizontal: false, vertical: true)
                            
                            Text(String(localized: "timeline.info.scroll_hint", table: "InfoPlist"))
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(nil)
                                .multilineTextAlignment(.leading)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
            }
            .padding(.horizontal, 16)
            
            if isExpanded {
            // Timeline content that extends edge-to-edge
            ZStack {
                // Continuous horizontal line across full width - positioned to pass through dots
                Rectangle()
                    .fill(Color.infoSecondary)
                    .frame(height: 3)
                    .frame(maxWidth: .infinity)
                    .position(x: UIScreen.main.bounds.width / 2, y: 80) // Position lower to pass through dots
                
                // Scrollable articles
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 0) {
                            ForEach(timelineArticles) { article in
                                TimelineArticleView(article: article)
                                    .frame(width: 200) // Fixed width for consistent spacing
                                    .id(article.id)
                            }
                        }
                        .padding(.horizontal, UIScreen.main.bounds.width / 2 - 100) // Center the current article (4th item)
                    }
                    .frame(height: 120) // Fixed height for the timeline
                    .onAppear {
                        // Scroll to center the current article
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                            proxy.scrollTo(currentArticle.articleID, anchor: .center)
                        }
                    }
                }
                
                // Coming soon overlay
                ComingSoonOverlay()
            }
            .padding(.horizontal, -20) // Extend timeline content to screen edges
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(String(localized: "timeline.accessibility.label", table: "InfoPlist"))
    }
}

// MARK: - Timeline Article Model
struct TimelineArticle: Identifiable {
    let id: String
    let title: String
    let date: String
    let articleID: String
    let isCurrent: Bool
    
    init(id: String, title: String, date: String, articleID: String, isCurrent: Bool = false) {
        self.id = id
        self.title = title
        self.date = date
        self.articleID = articleID
        self.isCurrent = isCurrent
    }
}

// MARK: - Timeline Article View
struct TimelineArticleView: View {
    let article: TimelineArticle
    
    private var formattedDate: String {
        // Match FeedArticleRow date format - relative time (e.g., "5 minutes ago", "2 hours ago")
        var date: Date?
        
        // Try ISO 8601 format with microseconds (6 digits) first
        let microsecondFormatter = DateFormatter()
        microsecondFormatter.locale = Locale(identifier: "en_US_POSIX")
        microsecondFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS"
        date = microsecondFormatter.date(from: article.date)
        
        // If that fails, try ISO 8601 with milliseconds (3 digits)
        if date == nil {
            let millisecondFormatter = DateFormatter()
            millisecondFormatter.locale = Locale(identifier: "en_US_POSIX")
            millisecondFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
            date = millisecondFormatter.date(from: article.date)
        }
        
        // If that fails, try ISO 8601 without fractional seconds
        if date == nil {
            let isoFormatter = ISO8601DateFormatter()
            isoFormatter.formatOptions = [.withInternetDateTime]
            date = isoFormatter.date(from: article.date)
        }
        
        // If that fails, try the old format "yyyy-MM-dd HH:mm:ss"
        if date == nil {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            date = formatter.date(from: article.date)
        }
        
        if let date = date {
            let now = Date()
            let timeInterval = now.timeIntervalSince(date)
            
            let minutes = Int(timeInterval / 60)
            let hours = Int(timeInterval / 3600)
            let days = Int(timeInterval / 86400)
            
            if minutes < 60 {
                return "\(minutes) minute\(minutes == 1 ? "" : "s") ago"
            } else if hours < 24 {
                return "\(hours) hour\(hours == 1 ? "" : "s") ago"
            } else {
                return "\(days) day\(days == 1 ? "" : "s") ago"
            }
        }
        return article.date
    }
    
    var body: some View {
        VStack(spacing: 8) {
            // Article title
            Text(article.title)
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.textPrimary)
                .lineLimit(3)
                .multilineTextAlignment(.center)
                .frame(height: 60) // Fixed height for consistent layout
            
            // Marker circle that sits on top of the timeline line
            ZStack {
                // Background circle to create the "line passing through" effect
                Circle()
                    .fill(Color.background)
                    .frame(width: article.isCurrent ? 20 : 16, height: article.isCurrent ? 20 : 16)
                
                // Main marker circle
                Circle()
                    .fill(article.isCurrent ? Color(hex: "#239b98") : Color.infoSecondary)
                    .frame(width: article.isCurrent ? 14 : 10, height: article.isCurrent ? 14 : 10)
            }
            .offset(y: -4) // Move dots slightly higher to align with timeline line
            
            // Date
            Text(formattedDate)
                .font(.caption2)
                .foregroundColor(Color.infoSecondary)
        }
        .frame(width: 180) // Slightly smaller than container for spacing
        .padding(.horizontal, 10)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(article.title), \(formattedDate)")
        .accessibilityHint(article.isCurrent ? String(localized: "timeline.article.current.hint", table: "InfoPlist") : String(localized: "timeline.article.related.hint", table: "InfoPlist"))
    }
}

// MARK: - Media Outlet Popup View
struct MediaOutletPopupView: View {
    let publisherID: Int?
    let publisherName: String
    let publisherIcon: String
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject private var appState: AppState
    @State private var showingSubscription = false
    @State private var isLoading: Bool = false
    @State private var errorMessage: String?
    @State private var publisherInfo: PublisherInfo?
    
    // Build a safe URL for the publisher website (adds https if missing)
    private var websiteURL: URL? {
        guard let website = publisherInfo?.website, !website.isEmpty else { return nil }
        if website.lowercased().hasPrefix("http") {
            return URL(string: website)
        }
        return URL(string: "https://\(website)")
    }
    
    private var headerTitle: String {
        if let name = publisherInfo?.name, !name.isEmpty {
            return name
        }
        return publisherName.isEmpty ? String(localized: "publisher.unknown", table: "InfoPlist") : publisherName
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                ScrollView {
                VStack(spacing: 20) {
                    // Header
                    HStack(spacing: 12) {
                        AsyncImage(url: URL(string: publisherIcon)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Circle()
                                .fill(Color.gray.opacity(0.2))
                                .overlay(Image(systemName: "newspaper").foregroundColor(.gray))
                        }
                        .frame(width: 56, height: 56)
                        .clipShape(Circle())
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text(headerTitle)
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundColor(.textPrimary)
                                .lineLimit(2)

                            if let stance = publisherInfo?.stance?.displayName,
                               !stance.isEmpty,
                               appState.showReportPatterns {
                                ZStack {
                                    Text(stance)
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(Color.gray.opacity(0.15))
                                        .foregroundColor(.textPrimary)
                                        .cornerRadius(6)
                                        .blur(radius: appState.isProUser ? 0 : 8)
                                    if !appState.isProUser {
                                        Image(systemName: "lock.fill")
                                            .font(.caption2)
                                            .fontWeight(.semibold)
                                            .foregroundColor(.white)
                                    }
                                }
                                .accessibilityLabel(String(localized: "publisher.info.stance", defaultValue: "Stance", table: "InfoPlist"))
                            }
                        }
                        Spacer()
                    }
                    
                    // Loading / Error / Content
                    if isLoading {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                    } else if let errorMessage = errorMessage {
                        VStack(spacing: 12) {
                            Text(errorMessage)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                            Button {
                                loadPublisherInfo()
                            } label: {
                                Text(String(localized: "button.retry", defaultValue: "Retry", table: "InfoPlist"))
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        .frame(maxWidth: .infinity)
                    } else if let info = publisherInfo {
                        VStack(alignment: .leading, spacing: 12) {
                            if !info.intro.isEmpty {
                                Text(info.intro)
                                    .font(.body)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.textPrimary)
                            }
                            
                            Grid(alignment: .leading, horizontalSpacing: 12, verticalSpacing: 10) {
                                GridRow {
                                    Text(String(localized: "publisher.info.region", defaultValue: "Region", table: "InfoPlist"))
                                        .font(.subheadline).foregroundColor(.secondary)
                                    Text(info.region).font(.subheadline).foregroundColor(.textPrimary)
                                }
                                GridRow {
                                    Text(String(localized: "publisher.info.type", defaultValue: "Type", table: "InfoPlist"))
                                        .font(.subheadline).foregroundColor(.secondary)
                                    Text(info.type).font(.subheadline).foregroundColor(.textPrimary)
                                }
                                if let conglomerate = info.conglomerate, !conglomerate.isEmpty {
                                    GridRow {
                                        Text(String(localized: "publisher.info.conglomerate", defaultValue: "Conglomerate", table: "InfoPlist"))
                                            .font(.subheadline).foregroundColor(.secondary)
                                        Text(conglomerate).font(.subheadline).foregroundColor(.textPrimary)
                                    }
                                }
                                if let controller = info.controller, !controller.isEmpty {
                                    GridRow {
                                        Text(String(localized: "publisher.info.controller", defaultValue: "Controller", table: "InfoPlist"))
                                            .font(.subheadline).foregroundColor(.secondary)
                                        Text(controller).font(.subheadline).foregroundColor(.textPrimary)
                                    }
                                }
                            }
                            
                            if let websiteURL = websiteURL {
                                Link(destination: websiteURL) {
                                    HStack(spacing: 10) {
                                        Image(systemName: "globe")
                                        Text(String(localized: "publisher.info.website", defaultValue: "Visit website", table: "InfoPlist"))
                                        Spacer()
                                    }
                                    .font(.subheadline)
                                    .foregroundColor(.secondary) // Greyscale text/icons
                                    .padding()
                                    .frame(maxWidth: .infinity)
                                    .background(Color.gray.opacity(0.12))
                                    .cornerRadius(10)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    } else {
                        Text(String(localized: "publisher.info.unavailable", defaultValue: "No publisher details available.", table: "InfoPlist"))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 20)
            }
            .blur(radius: appState.isProUser ? 0 : 12)
            .allowsHitTesting(appState.isProUser)
            
            if !appState.isProUser {
                Button {
                    showingSubscription = true
                } label: {
                    VStack(spacing: 12) {
                        Image(systemName: "lock.fill")
                            .font(.title)
                            .foregroundColor(.secondary)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.black.opacity(0.25))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(String(localized: "pro.paywall.publisher_info.title", defaultValue: "Publisher info is a Pro feature", table: "InfoPlist"))
            }
            }
            .navigationTitle("") // Name already shown beside logo; keep header minimal
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        dismiss()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title3)
                            .foregroundColor(.secondary)
                    }
                    .accessibilityLabel(String(localized: "button.close", defaultValue: "Close", table: "InfoPlist"))
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .sheet(isPresented: $showingSubscription) {
            SubscriptionView()
        }
        .task {
            loadPublisherInfo()
        }
    }
    
    private func loadPublisherInfo() {
        guard let publisherID = publisherID else {
            publisherInfo = nil
            return
        }
        isLoading = true
        errorMessage = nil
        
        Task {
            do {
                let info = try await NetworkService.shared.fetchPublisherInfo(publisherID: publisherID)
                await MainActor.run {
                    self.publisherInfo = info
                    self.isLoading = false
                }
            } catch {
                await MainActor.run {
                    self.errorMessage = error.localizedDescription
                    self.isLoading = false
                }
            }
        }
    }
}


// MARK: - News Detail Tutorial Overlay
struct NewsDetailTutorialOverlay: View {
    @Binding var currentStep: Int
    @Binding var isShowing: Bool
    let highlightFrames: [String: CGRect]
    let onComplete: () -> Void
    var onScrollToAnchor: ((String) -> Void)? = nil
    
    @State private var bubbleOpacity: Double = 0
    @State private var overlayOpacity: Double = 0
    @State private var lastScrollAnchor: String? = nil
    
    private let overlayColor = Color.black.opacity(0.7)
    private let bubbleBackgroundColor = Color(uiColor: UIColor { traitCollection in
        return traitCollection.userInterfaceStyle == .dark ?
            UIColor(red: 0.15, green: 0.15, blue: 0.15, alpha: 1.0) :
            UIColor(red: 0.98, green: 0.98, blue: 0.98, alpha: 1.0)
    })
    
    private var tutorialSteps: [(message: String, highlightKeys: [String], scrollAnchor: String)] {
        [
            (
                message: String(localized: "tutorial.newsdetail.step1", defaultValue: "Zone News summaries are generated from all the media reports on this event. Ask for more using the button below.", table: "InfoPlist"),
                highlightKeys: ["summaryContent", "summarySettingsButton", "generateContextButton"],
                scrollAnchor: "summaryParagraphs"
            ),
            (
                message: String(localized: "tutorial.newsdetail.step2", defaultValue: "This diagram shows media reporting patterns on this news event: who's reporting, their usual bias, and when.", table: "InfoPlist"),
                highlightKeys: ["mediaDistributionCard"],
                scrollAnchor: "mediaDistributionCard"
            ),
            (
                message: String(localized: "tutorial.newsdetail.step3", defaultValue: "This score represents how objective media reporting on this event is overall: a lower score is better.", table: "InfoPlist"),
                highlightKeys: ["subjectivityScoreCard"],
                scrollAnchor: "subjectivityScoreCard"
            ),
            (
                message: String(localized: "tutorial.newsdetail.step4", defaultValue: "Zone News is a tool to help you navigate the media environment, rather than a replacement for proper reporting. To know the full story, you should always read from the articles directly.", table: "InfoPlist"),
                highlightKeys: ["firstArticleCard"],
                scrollAnchor: "firstArticleCard"
            )
        ]
    }
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                overlayWithCutout(in: geometry)
                tutorialBubble(in: geometry)
                
                // Tap to continue hint at bottom - same as poster
                VStack {
                    Spacer()
                    
                    VStack(spacing: 8) {
                        Text(String(localized: "tutorial.continue.hint", defaultValue: "Tap anywhere to continue", table: "InfoPlist"))
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.white.opacity(0.7))
                        
                        Image(systemName: "chevron.down")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(.white.opacity(0.5))
                    }
                    .padding(.bottom, 60)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .opacity(bubbleOpacity)
            }
        }
        .ignoresSafeArea()
        .contentShape(Rectangle())
        .onTapGesture {
            advanceToNextStep()
        }
        .onAppear {
            showStepAnimation()
        }
        .onChange(of: currentStep) { _, _ in
            showStepAnimation()
        }
    }
    
    @ViewBuilder
    private func overlayWithCutout(in geometry: GeometryProxy) -> some View {
        let step = tutorialSteps[currentStep]
        
        let validFrames = step.highlightKeys.compactMap { key -> CGRect? in
            guard let frame = highlightFrames[key] else { return nil }
            return CGRect(
                x: frame.minX - 8,
                y: frame.minY - 8,
                width: frame.width + 16,
                height: frame.height + 16
            )
        }
        
        if !validFrames.isEmpty {
            Canvas { context, size in
                context.fill(
                    Path(CGRect(origin: .zero, size: size)),
                    with: .color(overlayColor)
                )
                
                context.blendMode = .destinationOut
                for frame in validFrames {
                    let cutoutPath = Path(roundedRect: frame, cornerRadius: 12)
                    context.fill(cutoutPath, with: .color(.white))
                }
            }
            .compositingGroup()
            .opacity(overlayOpacity)
        } else {
            overlayColor
                .opacity(overlayOpacity)
        }
    }
    
    @ViewBuilder
    private func tutorialBubble(in geometry: GeometryProxy) -> some View {
        let step = tutorialSteps[currentStep]
        let bubbleWidth = min(geometry.size.width - 48, 320)
        let bubblePosition = calculateBubblePosition(in: geometry, bubbleWidth: bubbleWidth)
        
        VStack(alignment: .leading, spacing: 12) {
            Text(step.message)
                .font(.subheadline)
                .foregroundColor(.primary)
                .lineLimit(nil)
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(width: bubbleWidth)
        .padding(.horizontal, 16)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(bubbleBackgroundColor)
                .shadow(color: .black.opacity(0.25), radius: 12, x: 0, y: 4)
        )
        .position(x: bubblePosition.x, y: bubblePosition.y)
        .opacity(bubbleOpacity)
    }
    
    private func calculateBubblePosition(in geometry: GeometryProxy, bubbleWidth: CGFloat) -> CGPoint {
        let step = tutorialSteps[currentStep]
        let horizontalCenter = geometry.size.width / 2
        let estimatedBubbleHeight: CGFloat = 120
        let padding: CGFloat = 24
        let safeAreaTop = geometry.safeAreaInsets.top

        // For step 1, anchor the bubble beneath the get-context button
        if step.highlightKeys.contains("generateContextButton"),
           let contextFrame = highlightFrames["generateContextButton"] {
            let y = contextFrame.maxY + padding + estimatedBubbleHeight / 2 + 16
            return CGPoint(x: horizontalCenter, y: min(y, geometry.size.height - 100))
        }
        
        // Get all valid frames for the current step
        let validFrames = step.highlightKeys.compactMap { key -> CGRect? in
            highlightFrames[key]
        }
        
        if let firstFrame = validFrames.first {
            // Calculate combined bounding box if multiple frames
            let combinedFrame = validFrames.reduce(firstFrame) { result, frame in
                CGRect(
                    x: min(result.minX, frame.minX),
                    y: min(result.minY, frame.minY),
                    width: max(result.maxX, frame.maxX) - min(result.minX, frame.minX),
                    height: max(result.maxY, frame.maxY) - min(result.minY, frame.minY)
                )
            }
            
            // Try to position above the highlight if possible
            let spaceAbove = combinedFrame.minY - safeAreaTop
            if spaceAbove > estimatedBubbleHeight + padding * 2 {
                let y = combinedFrame.minY - padding - estimatedBubbleHeight / 2 - 16
                return CGPoint(x: horizontalCenter, y: max(y, safeAreaTop + estimatedBubbleHeight / 2 + padding))
            } else {
                // Position below
                let y = combinedFrame.maxY + padding + estimatedBubbleHeight / 2 + 16
                return CGPoint(x: horizontalCenter, y: min(y, geometry.size.height - 100))
            }
        } else {
            return CGPoint(x: horizontalCenter, y: geometry.size.height / 2)
        }
    }
    
    private func advanceToNextStep() {
        bubbleOpacity = 0
        overlayOpacity = 0
        
        if currentStep < tutorialSteps.count - 1 {
            currentStep += 1
        } else {
            isShowing = false
            onComplete()
        }
    }
    
    private func showStepAnimation() {
        bubbleOpacity = 0
        overlayOpacity = 0
        
        // Check if scroll anchor changed and trigger scroll
        if currentStep < tutorialSteps.count {
            let newAnchor = tutorialSteps[currentStep].scrollAnchor
            if lastScrollAnchor != newAnchor {
                onScrollToAnchor?(newAnchor)
                lastScrollAnchor = newAnchor
            }
        }
        
        // Delay fade-in to allow scroll to complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            withAnimation(.easeIn(duration: 0.25)) {
                overlayOpacity = 1
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                withAnimation(.easeIn(duration: 0.2)) {
                    bubbleOpacity = 1
                }
            }
        }
    }
}

// MARK: - ScrollView Accessor
struct ScrollViewAccessor: UIViewControllerRepresentable {
    let savedOffset: CGFloat
    let shouldRestore: Bool
    let onOffsetChange: (CGFloat) -> Void
    let onRestoreComplete: () -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        return context.coordinator.host
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        DispatchQueue.main.async {
            if let scrollView = context.coordinator.findScrollView(startingFrom: uiViewController) {
                // Set up delegate to track scroll position
                if context.coordinator.scrollView !== scrollView {
                    context.coordinator.scrollView?.delegate = nil
                    context.coordinator.scrollView = scrollView
                    scrollView.delegate = context.coordinator
                }
                
                // Restore scroll position if needed
                if shouldRestore && savedOffset > 0 {
                    context.coordinator.restoreScrollPosition(
                        scrollView: scrollView,
                        savedOffset: savedOffset,
                        onComplete: onRestoreComplete
                    )
                }
            }
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(onOffsetChange: onOffsetChange)
    }
    
    class Coordinator: NSObject, UIScrollViewDelegate {
        let host = UIViewController()
        weak var scrollView: UIScrollView?
        let onOffsetChange: (CGFloat) -> Void
        private var restoreAttempts = 0
        private let maxRestoreAttempts = 10
        
        init(onOffsetChange: @escaping (CGFloat) -> Void) {
            self.onOffsetChange = onOffsetChange
        }
        
        func findScrollView(startingFrom controller: UIViewController) -> UIScrollView? {
            // Search in the view hierarchy
            return findScrollView(in: controller.view)
        }
        
        private func findScrollView(in view: UIView) -> UIScrollView? {
            if let scrollView = view as? UIScrollView {
                return scrollView
            }
            for subview in view.subviews {
                if let scrollView = findScrollView(in: subview) {
                    return scrollView
                }
            }
            return nil
        }
        
        func restoreScrollPosition(scrollView: UIScrollView, savedOffset: CGFloat, onComplete: @escaping () -> Void) {
            restoreAttempts = 0
            attemptRestore(scrollView: scrollView, savedOffset: savedOffset, onComplete: onComplete)
        }
        
        private func attemptRestore(scrollView: UIScrollView, savedOffset: CGFloat, onComplete: @escaping () -> Void) {
            let contentHeight = scrollView.contentSize.height
            let scrollViewHeight = scrollView.bounds.height
            
            if contentHeight > 0 && scrollViewHeight > 0 {
                let maxOffset = max(0, contentHeight - scrollViewHeight)
                let targetOffset = min(savedOffset, maxOffset)
                scrollView.setContentOffset(CGPoint(x: 0, y: targetOffset), animated: false)
                restoreAttempts = 0
                onComplete()
            } else if restoreAttempts < maxRestoreAttempts {
                restoreAttempts += 1
                // Retry with increasing delays
                let delay = min(0.1 * Double(restoreAttempts), 0.5)
                DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                    self.attemptRestore(scrollView: scrollView, savedOffset: savedOffset, onComplete: onComplete)
                }
            } else {
                // Give up after max attempts
                restoreAttempts = 0
                onComplete()
            }
        }
        
        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            onOffsetChange(scrollView.contentOffset.y)
        }
    }
}

// MARK: - Coming Soon Overlay
struct ComingSoonOverlay: View {
    var body: some View {
        ZStack {
            // Material blur background with subtle tint
            Rectangle()
                .fill(.ultraThinMaterial)
                .background(
                    Color.primary.opacity(0.03)
                )
            
            // Content
            VStack(spacing: 10) {
                Image(systemName: "sparkles")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [
                                Color(hex: "#239b98"),
                                Color(hex: "#239b98").opacity(0.8)
                            ],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .symbolEffect(.pulse, options: .repeating.speed(0.5))
                
                VStack(spacing: 4) {
                    Text(String(localized: "coming.soon.overlay.title", defaultValue: "Coming soon", table: "InfoPlist"))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.primary)
                    
                    Text(String(localized: "coming.soon.overlay.message", defaultValue: "This feature will be available soon", table: "InfoPlist"))
                        .font(.system(size: 13, weight: .regular))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .allowsHitTesting(false)
    }
}

#Preview {
    NewsDetailView(articleID: "123")
        .environmentObject(AppState())
}
