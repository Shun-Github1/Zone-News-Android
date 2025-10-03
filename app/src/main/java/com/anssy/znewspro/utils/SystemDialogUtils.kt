package com.anssy.znewspro.utils

import android.app.ProgressDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.anssy.znewspro.R

/**
 * Utility class for system-style loading dialogs and notifications
 * Replaces WeChat-style DialogX components with Android system components
 */
object SystemDialogUtils {
    
    private var progressDialog: ProgressDialog? = null
    
    /**
     * Show a system-style loading dialog
     * @param context The context to show the dialog in
     * @param message The message to display
     */
    @Suppress("DEPRECATION")
    fun showLoadingDialog(context: Context, message: String) {
        dismissLoadingDialog()
        progressDialog = ProgressDialog(context).apply {
            setMessage(message)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }
    
    /**
     * Dismiss the current loading dialog
     */
    fun dismissLoadingDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    
    /**
     * Show a success message using system Toast
     * @param context The context to show the toast in
     * @param message The success message to display
     */
    fun showSuccessMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show an error message using system Toast
     * @param context The context to show the toast in
     * @param message The error message to display
     */
    fun showErrorMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * Show a system-style alert dialog for important messages
     * @param context The context to show the dialog in
     * @param title The dialog title
     * @param message The dialog message
     * @param positiveButtonText Text for the positive button (default: "OK")
     * @param onPositiveClick Callback for positive button click
     */
    fun showAlertDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = context.getString(R.string.ok),
        onPositiveClick: (() -> Unit)? = null
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.custom_alert_dialog, null)
        
        // Set title and message
        dialogView.findViewById<TextView>(R.id.dialog_title).text = title
        dialogView.findViewById<TextView>(R.id.dialog_message).text = message
        
        // Set button text
        dialogView.findViewById<MaterialButton>(R.id.btn_positive).text = positiveButtonText
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Remove the default dialog background to show only our rounded card
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set up button click listeners
        dialogView.findViewById<MaterialButton>(R.id.btn_negative).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<MaterialButton>(R.id.btn_positive).setOnClickListener {
            onPositiveClick?.invoke()
            dialog.dismiss()
        }
        
        dialog.show()
    }
}