import SwiftUI

// MARK: - Enhanced News Detail View
struct NewsDetailView: View {
    let articleID: String
    @Environment(\.dismiss) var dismiss
    @StateObject private var viewModel = NewsDetailViewModel()
    @State private var showingShareSheet = false
    @State private var showingFeedbackSheet = false
    @State private var feedbackText = ""
    
    var body: some View {
        NavigationStack {
            mainContent
                .navigationBarTitleDisplayMode(.inline)
                .navigationBarBackButtonHidden(true)
                .toolbar {
                    toolbarContent
                }
                .sheet(isPresented: $showingShareSheet) {
                    shareSheet
                }
                .sheet(isPresented: $showingFeedbackSheet) {
                    feedbackSheet
                }
                .alert("Feedback", isPresented: .constant(!viewModel.feedbackMessage.isEmpty)) {
                    Button("OK") {
                        viewModel.feedbackMessage = ""
                    }
                } message: {
                    Text(viewModel.feedbackMessage)
                }
                .onAppear {
                    viewModel.loadArticleDetail(articleID: articleID)
                }
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
        ProgressView("Loading article...")
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
    
    private func articleContentView(article: DetailedArticle) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                headerImageView(article: article)
                articleBodyView(article: article)
            }
        }
    }
    
    private func headerImageView(article: DetailedArticle) -> some View {
        Group {
            if !article.pictureURL.isEmpty {
                AsyncImage(url: URL(string: article.pictureURL)) { image in
                    image
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    Rectangle()
                        .fill(Color.gray.opacity(0.3))
                        .overlay(
                            ProgressView()
                        )
                }
                .frame(maxWidth: .infinity, maxHeight: 250)
                .clipped()
            }
        }
    }
    
    private func articleBodyView(article: DetailedArticle) -> some View {
        VStack(alignment: .leading, spacing: 20) {
            articleSummarySection(article: article)
            SentimentMeterCard(sentiment: article.metrics.sentiment)
            PublisherDistributionCard(coverage: article.coverage)
            SubjectivityScoreCard(subjectivity: article.metrics.subjectivity)
            if !article.articles.isEmpty {
                PublisherArticlesListCard(articles: article.articles)
            }
        }
        .padding(20)
    }
    
    private func articleSummarySection(article: DetailedArticle) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            articleTitleAndDescription(article: article)
            generateContextButton(article: article)
            feedbackButtonRow
        }
    }
    
    private func articleTitleAndDescription(article: DetailedArticle) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(article.title)
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(.text151515)
                .lineLimit(nil)
                .accessibilityAddTraits(.isHeader)
            
            if !article.description.isEmpty {
                Text(article.description)
                    .font(.body)
                    .foregroundColor(.text666)
                    .lineLimit(nil)
            }
        }
    }
    
    private func generateContextButton(article: DetailedArticle) -> some View {
        Button(action: {
            print("Generate Context tapped for article: \(article.articleID)")
        }) {
            HStack(spacing: 8) {
                Image(systemName: "bubble.left.and.bubble.right")
                    .font(.body)
                Text("Generate Context")
                    .font(.body)
                    .fontWeight(.medium)
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .background(Color.globalColor)
            .cornerRadius(8)
        }
        .frame(maxWidth: .infinity)
        .scaleEffect(0.8, anchor: .center)
        .accessibilityLabel("Generate Context")
        .accessibilityHint("Generates context information for this article")
    }
    
    private var feedbackButtonRow: some View {
        HStack {
            Spacer()
            Button(action: {
                showingFeedbackSheet = true
            }) {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.bubble")
                        .font(.caption)
                    Text("Feedback")
                        .font(.caption)
                }
                .foregroundColor(.globalColor)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .overlay(
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(Color.globalColor, lineWidth: 1)
                )
            }
            .accessibilityLabel("Provide Feedback")
            .accessibilityHint("Submits feedback about this article")
        }
    }
    
    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundColor(.orange)
            
            Text("Error Loading Article")
                .font(.headline)
            
            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            
            Button("Retry") {
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
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
            }
            .accessibilityLabel("Back")
        }
        
        ToolbarItem(placement: .navigationBarTrailing) {
            HStack(spacing: 16) {
                saveButton
                shareButton
            }
        }
    }
    
    private var saveButton: some View {
        Button(action: {
            viewModel.toggleLiked()
        }) {
            Image(systemName: viewModel.isLiked ? "heart.fill" : "heart")
                .font(.body)
                .foregroundColor(viewModel.isLiked ? .red : .primary)
        }
        .accessibilityLabel(viewModel.isLiked ? "Unsave Article" : "Save Article")
        .accessibilityHint(viewModel.isLiked ? "Removes this article from saved items" : "Saves this article for later")
    }
    
    private var shareButton: some View {
        Button(action: {
            showingShareSheet = true
            Task {
                await viewModel.trackShareAction()
            }
        }) {
            Image(systemName: "square.and.arrow.up")
                .font(.body)
                .foregroundColor(.primary)
        }
        .accessibilityLabel("Share Article")
        .accessibilityHint("Shares this article with others")
    }
    
    private var shareSheet: some View {
        Group {
            if let article = viewModel.detailedArticle {
                ShareSheet(items: [URL(string: article.shareURL)].compactMap { $0 })
            }
        }
    }
    
    private var feedbackSheet: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Submit Feedback")
                    .font(.headline)
                
                TextEditor(text: $feedbackText)
                    .frame(minHeight: 100)
                    .padding(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                    )
                
                Button("Submit") {
                    Task {
                        let success = await viewModel.submitFeedback(content: feedbackText)
                        if success {
                            feedbackText = ""
                            showingFeedbackSheet = false
                        }
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(feedbackText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                
                Spacer()
            }
            .padding()
            .navigationTitle("Feedback")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        showingFeedbackSheet = false
                    }
                }
            }
        }
    }
}

// MARK: - Sentiment Meter Card
struct SentimentMeterCard: View {
    let sentiment: Double
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Sentiment Score")
                .font(.headline)
                .foregroundColor(.text333)
            
            ZStack {
                // Background semi-circle
                Path { path in
                    let center = CGPoint(x: 100, y: 100)
                    let radius: CGFloat = 80
                    path.addArc(center: center, radius: radius + 6, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                }
                .stroke(Color.gray.opacity(0.2), lineWidth: 2)
                
                Path { path in
                    let center = CGPoint(x: 100, y: 100)
                    let radius: CGFloat = 80
                    path.addArc(center: center, radius: radius - 6, startAngle: .degrees(180), endAngle: .degrees(0), clockwise: false)
                }
                .stroke(Color.gray.opacity(0.2), lineWidth: 2)
                
                Path { path in
                    let start = CGPoint(x: 100 + 80 - 6, y: 100)
                    let end = CGPoint(x: 100 + 80 - 6 + 12, y: 100)
                    path.move(to: start)
                    path.addLine(to: end)
                }
                .stroke(Color.gray.opacity(0.2), lineWidth: 2)
                
                Path { path in
                    let start = CGPoint(x: 100 - 80 + 6, y: 100)
                    let end = CGPoint(x: 100 - 80 + 6 - 12, y: 100)
                    path.move(to: start)
                    path.addLine(to: end)
                }
                .stroke(Color.gray.opacity(0.2), lineWidth: 2)
                
                // Sentiment arc
                if sentiment != 0 {
                    Path { path in
                        let center = CGPoint(x: 100, y: 100)
                        let radius: CGFloat = 80
                        let angle = abs(sentiment) * 90 // Convert -1 to 1 range to 0 to 180 degrees
                        
                        if sentiment < 0 {
                            // Anti-clockwise (negative sentiment)
                            path.addArc(center: center, radius: radius, startAngle: .degrees(270), endAngle: .degrees(270 - angle), clockwise: true) // reversed, because up-side down
                        } else {
                            // Clockwise (positive sentiment)
                            path.addArc(center: center, radius: radius, startAngle: .degrees(270), endAngle: .degrees(270 + angle), clockwise: false)
                        }
                    }
                    .stroke(sentiment < 0 ? Color(hex: "#7F2538") : Color(hex: "#239B98"), lineWidth: 12)
                    .animation(.easeInOut(duration: 2), value: sentiment)
                }
                
                // Labels
                Text("0")
                    .font(.caption)
                    .foregroundColor(.text666)
                    .position(x: 100, y: 0)
                
                Text("-1")
                    .font(.caption)
                    .foregroundColor(.text666)
                    .position(x: 20, y: 110)
                
                Text("+1")
                    .font(.caption)
                    .foregroundColor(.text666)
                    .position(x: 180, y: 110)
                
                // Current value indicator
                let sentiment_val_string = (sentiment > 0 ? "+" : "") + String(format: "%.2f", sentiment)
                
                VStack {
                    Text(sentiment_val_string)
                        .font(.custom("BigDataField", size: 45))
                        .fontWeight(.medium)
                        .foregroundColor(sentiment < 0 ? Color(hex: "#7F2538") : (sentiment > 0 ? Color(hex: "#239B98") : .text666))
                }
                .position(x: 100, y: 85)
            }
            .frame(width: 200, height: 120)
            .frame(maxWidth: .infinity)
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Sentiment Score")
        .accessibilityValue("\(String(format: "%.1f", sentiment)) out of 1")
    }
}

// MARK: - Subjectivity Score Card
struct SubjectivityScoreCard: View {
    let subjectivity: Double
    @State private var showingInfo: Bool = false

    private var formattedValue: String {
        // Display to 2 significant figures, as required
        String(format: "%.2g", subjectivity)
    }

    private var statusText: String {
        if subjectivity > 0.66 { return "Low objectivity" }
        if subjectivity > 0.33 && subjectivity < 0.66 { return "Medium objectivity" }
        if subjectivity < 0.33 { return "High objectivity" }
        return "Medium objectivity"
    }

    private var statusTextColor: Color {
        if subjectivity > 0.66 { return Color(hex: "#879693") }
        if subjectivity > 0.33 && subjectivity < 0.66 { return Color(hex: "#9AEDDD") }
        if subjectivity < 0.33 { return Color(hex: "#239B98") }
        return Color(hex: "#9AEDDD")
    }

    private var statusBackgroundColor: Color {
        if subjectivity > 0.66 { return Color(hex: "#2E3D3A") }
        if subjectivity > 0.33 && subjectivity < 0.66 { return Color(hex: "#3D776C") }
        if subjectivity < 0.33 { return Color(hex: "#9AEDDD") }
        return Color(hex: "#3D776C")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .center) {
                Text("Subjectivity Score")
                    .font(.headline)
                    .foregroundColor(.text333)
                Spacer()
                Button {
                    showingInfo = true
                } label: {
                    Image(systemName: "info.circle")
                        .foregroundColor(.text666)
                }
                .accessibilityLabel("Subjectivity Score Information")
                .popover(isPresented: $showingInfo) {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Determined by a stolen algorithm from a trust-worthy, state-sponsored, mentally abused PhD researcher.")
                            .font(.body)
                            .foregroundColor(.text333)
                        HStack(spacing: 4) {
                            Text("Visit")
                                .foregroundColor(.text333)
                            Link("our webpage", destination: URL(string: "https://www.example.com")!)
                            Text("for detailed information.")
                                .foregroundColor(.text333)
                        }
                        .font(.body)
                    }
                    .padding()
                    .presentationCompactAdaptation(.popover)
                }
            }

            HStack(alignment: .center, spacing: 12) {
                // Main data text with denominator in bottom-right
                ZStack(alignment: .bottomTrailing) {
                    Text(formattedValue)
                        .font(.custom("BigDataField", size: 45))
                        .fontWeight(.medium)
                        .foregroundColor(.text333)
                        .padding(.leading, 5)
                        .padding(.trailing, 12)
                    Text("/1")
                        .font(.caption2)
                        .fontWeight(.light)
                        .foregroundColor(.text666)
                        .padding(.trailing, 2)
                        .padding(.bottom, 6)
                }
                .frame(minWidth: 80, alignment: .leading)

                Spacer(minLength: 0)

                // Status text with fixed-width rectangular background
                Text(statusText)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundColor(statusTextColor)
                    .frame(width: 160, height: 32)
                    .background(statusBackgroundColor)
                    .cornerRadius(4)
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Subjectivity Score")
        .accessibilityValue("\(formattedValue) out of 1")
    }
}

// MARK: - Publisher Distribution Card
struct PublisherDistributionCard: View {
    let coverage: DetailedArticleCoverage
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Publisher Distribution")
                .font(.headline)
                .foregroundColor(.text333)
            
            GeometryReader { geometry in
                let width = geometry.size.width
                let height: CGFloat = 120
                let separationX = width * coverage.percentage.centric
                
                ZStack {
                    // Background rectangles
                    HStack(spacing: 0) {
                        Rectangle()
                            .fill(Color(hex: "#999999"))
                            .frame(width: separationX)
                        
                        Rectangle()
                            .fill(Color(hex: "#9aeddd"))
                            .frame(width: width - separationX)
                    }
                    .frame(height: height)
                    .cornerRadius(8)
                    
                    // Separation line
                    Rectangle()
                        .fill(Color.white.opacity(1.0))
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
            
            // Legend
            HStack {
                HStack(spacing: 4) {
                    Rectangle()
                        .fill(Color(hex: "#999999"))
                        .frame(width: 12, height: 12)
                        .cornerRadius(2)
                    Text("Centric")
                        .font(.caption)
                        .foregroundColor(.text666)
                }
                
                Spacer()
                
                HStack(spacing: 4) {
                    Rectangle()
                        .fill(Color(hex: "#9aeddd"))
                        .frame(width: 12, height: 12)
                        .cornerRadius(2)
                    Text("Progressive")
                        .font(.caption)
                        .foregroundColor(.text666)
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Publisher Distribution")
        .accessibilityValue("\(Int(coverage.percentage.centric * 100))% centric, \(Int(coverage.percentage.progressive * 100))% progressive")
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
                .fill(Color.gray.opacity(0.3))
                .overlay(
                    Image(systemName: "building.2")
                        .font(.caption)
                        .foregroundColor(.gray)
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
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Publisher Articles")
                .font(.headline)
                .foregroundColor(.text333)
            
            VStack(spacing: 0) {
                ForEach(Array(articles.enumerated()), id: \.offset) { index, publisherArticle in
                    PublisherArticleRowView(article: publisherArticle)
                    if index < articles.count - 1 {
                        Divider()
                            .padding(.leading, 56)
                    }
                }
            }
        }
        .padding(16)
        .background(Color.white)
        .cornerRadius(8)
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Publisher Articles")
    }
}

// MARK: - Publisher Article Row View
struct PublisherArticleRowView: View {
    let article: PublisherArticle
    
    private var stanceColor: Color {
        let value = article.publisherStance.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if value == "progressive" { return Color(hex: "#9AEDDD") }
        if value == "conservative" || value == "centric" { return Color(hex: "#999999") }
        return .text666
    }
    
    var body: some View {
        Group {
            if let url = URL(string: article.articleURL), !article.articleURL.isEmpty {
                Link(destination: url) {
                    rowContent
                }
                .buttonStyle(.plain)
            } else {
                rowContent
                    .opacity(0.6)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(article.publisherName), \(article.title)")
    }
    
    private var rowContent: some View {
        HStack(alignment: .top, spacing: 12) {
            AsyncImage(url: URL(string: article.publisherIcon)) { image in
                image
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } placeholder: {
                Circle()
                    .fill(Color.gray.opacity(0.3))
                    .overlay(
                        Image(systemName: "newspaper")
                            .font(.caption)
                            .foregroundColor(.gray)
                    )
            }
            .frame(width: 40, height: 40)
            .clipShape(Circle())
            
            VStack(alignment: .leading, spacing: 6) {
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text(article.publisherName.isEmpty ? "Unknown Publisher" : article.publisherName)
                        .font(.subheadline)
                        .foregroundColor(.text666)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    
                    if !article.publisherStance.isEmpty {
                        Text(article.publisherStance)
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(stanceColor)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(stanceColor.opacity(0.15))
                            .cornerRadius(4)
                    }
                }
                
                Text(article.title)
                    .font(.body)
                    .foregroundColor(.text151515)
                    .lineLimit(3)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.footnote)
                .foregroundColor(.text999)
                .padding(.top, 2)
        }
        .contentShape(Rectangle())
        .padding(.vertical, 10)
    }
}

#Preview {
    NewsDetailView(articleID: "123")
}
