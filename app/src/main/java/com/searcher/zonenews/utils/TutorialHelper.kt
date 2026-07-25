package com.searcher.zonenews.utils

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.searcher.zonenews.R
import com.searcher.zonenews.selfview.TutorialOverlayView

/**
 * Helper class to show tutorials on different screens
 * Provides convenient methods for showing tutorials with minimal boilerplate
 */
object TutorialHelper {

    /**
     * Show tutorial for the Home page (main news feed)
     */
    fun showHomeTutorial(
        fragment: Fragment,
        rootView: ViewGroup,
        scrollView: NestedScrollView?,
        getFirstNewsRow: () -> View?,
        getSentimentBar: () -> View?,
        onComplete: () -> Unit = {}
    ) {
        val context = fragment.requireContext()
        
        // Check if tutorial was already shown
        if (TutorialManager.hasTutorialBeenShown(context, TutorialManager.TUTORIAL_HOME)) {
            return
        }
        
        val steps = listOf(
            TutorialOverlayView.TutorialStep(
                id = "home_welcome",
                message = context.getString(R.string.tutorial_home_step1_message),
                hasHighlight = false
            ),
            TutorialOverlayView.TutorialStep(
                id = "home_news_row",
                message = context.getString(R.string.tutorial_home_step1),
                hasHighlight = true
            ),
            TutorialOverlayView.TutorialStep(
                id = "home_sentiment",
                message = context.getString(R.string.tutorial_home_sentiment_bar),
                hasHighlight = true
            )
        )
        
        showTutorial(
            rootView = rootView,
            steps = steps,
            scrollView = scrollView,
            getTargetView = { step ->
                when (step.id) {
                    "home_news_row" -> getFirstNewsRow()
                    "home_sentiment" -> getSentimentBar()
                    else -> null
                }
            },
            onComplete = {
                TutorialManager.markTutorialAsShown(context, TutorialManager.TUTORIAL_HOME)
                onComplete()
            }
        )
    }
    
    /**
     * Show tutorial for the Your Feed (Personal) page
     */
    fun showYourFeedTutorial(
        fragment: Fragment,
        rootView: ViewGroup,
        scrollView: NestedScrollView?,
        getActiveTagsHeader: () -> View?,
        getRecapButton: () -> View?,
        getLevityButton: () -> View?,
        onComplete: () -> Unit = {}
    ) {
        val context = fragment.requireContext()
        
        // Check if tutorial was already shown
        if (TutorialManager.hasTutorialBeenShown(context, TutorialManager.TUTORIAL_YOUR_FEED)) {
            return
        }
        
        val steps = mutableListOf<TutorialOverlayView.TutorialStep>()
        
        // Only add active tags step if tags are visible
        steps.add(
            TutorialOverlayView.TutorialStep(
                id = "personal_active_tags",
                message = context.getString(R.string.tutorial_personal_active_tags),
                hasHighlight = true,
                scrollPosition = 0
            )
        )
        
        // Recap button step - check if it exists
        getRecapButton()?.let {
            steps.add(
                TutorialOverlayView.TutorialStep(
                    id = "personal_recap",
                    message = context.getString(R.string.tutorial_personal_recap),
                    hasHighlight = true
                )
            )
        }
        
        // Levity button step - check if it exists
        getLevityButton()?.let {
            steps.add(
                TutorialOverlayView.TutorialStep(
                    id = "personal_levity",
                    message = context.getString(R.string.tutorial_personal_levity),
                    hasHighlight = true
                )
            )
        }
        
        if (steps.isEmpty()) {
            TutorialManager.markTutorialAsShown(context, TutorialManager.TUTORIAL_YOUR_FEED)
            onComplete()
            return
        }
        
        showTutorial(
            rootView = rootView,
            steps = steps,
            scrollView = scrollView,
            getTargetView = { step ->
                when (step.id) {
                    "personal_active_tags" -> getActiveTagsHeader()
                    "personal_recap" -> getRecapButton()
                    "personal_levity" -> getLevityButton()
                    else -> null
                }
            },
            onComplete = {
                TutorialManager.markTutorialAsShown(context, TutorialManager.TUTORIAL_YOUR_FEED)
                onComplete()
            }
        )
    }
    
    /**
     * Show tutorial for the News Detail page
     */
    fun showNewsDetailTutorial(
        activity: Activity,
        rootView: ViewGroup,
        scrollView: NestedScrollView?,
        getSettingsButton: () -> View?,
        getMediaDistributionCard: () -> View?,
        getSubjectivityCard: () -> View?,
        getFirstArticleCard: () -> View?,
        onComplete: () -> Unit = {}
    ) {
        // Check if tutorial was already shown
        if (TutorialManager.hasTutorialBeenShown(activity, TutorialManager.TUTORIAL_NEWS_DETAIL)) {
            return
        }
        
        val steps = listOf(
            TutorialOverlayView.TutorialStep(
                id = "detail_settings",
                message = activity.getString(R.string.tutorial_detail_summary_settings),
                hasHighlight = true,
                scrollPosition = 0
            ),
            TutorialOverlayView.TutorialStep(
                id = "detail_distribution",
                message = activity.getString(R.string.tutorial_detail_media_distribution),
                hasHighlight = true,
                scrollPosition = 400
            ),
            TutorialOverlayView.TutorialStep(
                id = "detail_subjectivity",
                message = activity.getString(R.string.tutorial_detail_subjectivity),
                hasHighlight = true,
                scrollPosition = 600
            ),
            TutorialOverlayView.TutorialStep(
                id = "detail_article",
                message = activity.getString(R.string.tutorial_detail_article_card),
                hasHighlight = true,
                scrollPosition = 1200
            )
        )
        
        showTutorial(
            rootView = rootView,
            steps = steps,
            scrollView = scrollView,
            getTargetView = { step ->
                when (step.id) {
                    "detail_settings" -> getSettingsButton()
                    "detail_distribution" -> getMediaDistributionCard()
                    "detail_subjectivity" -> getSubjectivityCard()
                    "detail_article" -> getFirstArticleCard()
                    else -> null
                }
            },
            onComplete = {
                TutorialManager.markTutorialAsShown(activity, TutorialManager.TUTORIAL_NEWS_DETAIL)
                onComplete()
            }
        )
    }
    
    /**
     * Core method to display a tutorial overlay
     */
    private fun showTutorial(
        rootView: ViewGroup,
        steps: List<TutorialOverlayView.TutorialStep>,
        scrollView: NestedScrollView?,
        getTargetView: (TutorialOverlayView.TutorialStep) -> View?,
        onComplete: () -> Unit
    ) {
        // Create overlay
        val overlay = TutorialOverlayView(rootView.context)
        overlay.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Set up steps
        overlay.setTutorialSteps(steps)
        overlay.setOnTutorialCompleteListener(onComplete)
        
        // Add to root view
        rootView.addView(overlay)
        
        // Store callbacks for advancing
        val advanceCallback = { step: TutorialOverlayView.TutorialStep -> getTargetView(step) }
        
        // Set up click listener that advances with proper callback
        overlay.setOnClickListener {
            overlay.advanceWithCallback(scrollView, advanceCallback)
        }
        
        // Start tutorial
        overlay.start(scrollView, getTargetView)
    }
}
