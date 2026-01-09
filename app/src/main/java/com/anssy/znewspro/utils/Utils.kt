package com.anssy.znewspro.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.util.TypedValue
import android.view.inputmethod.InputMethodManager
import com.anssy.znewspro.R
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


object Utils {
    @JvmStatic
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    fun dip2px(dpValue: Float, context: Context): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 将字节数转换成MB的函数
     * @param bytes 输入的字节数
     * @return 转换后的MB数
     */
    fun convertBytesToMB(bytes: Long): Double {
        // 进行转换计算
        return bytes.toDouble() / (1024 * 1024) // 将字节数转换为MB
    }
    /**
     *
     * 获取两个时间段的分钟差
     *
     *
     *
     * @param startDate 年月日时分秒
     *
     * @param endDate
     *
     * @return
     */
    fun getGapMinutes(startDate: String?, endDate: String?): Int {
        var start: Long = 0
        var end: Long = 0
        try {
            val df =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            start = df.parse(startDate ?: "")?.time ?: 0
            end = df.parse(endDate ?: "")?.time ?: 0
        } catch (e: Exception) {
// TODO: handle exception
        }

//        CLog.e("开始结束时间1", (end - start) + "");
        return ((end - start) / (1000 * 60)).toInt()
    }

    fun gmtFormat(strDate: String): Date? {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT") // 设置时区为GMT
        // 解析字符串为Date对象
        val dateTime = sdf.parse(strDate)
        return dateTime
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun dpToPx(dp: Float, resources: Resources): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }
    @JvmStatic
    fun isGoodJson(json: String?): Boolean {
        return try {
            JsonParser().parse(json)
            true
        } catch (e: JsonParseException) {
            false
        }
    }

    fun getStatusBarHeight(context: Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    /**
     * 控制输入格式，保留两位小数
     *
     * @param edt Editable
     */
    fun decimal(edt: Editable?) {
        if (edt == null) {
            return
        }

        // 以小数点开头，前面自动加上 "0"
        if (edt.toString().startsWith(".")) {
            edt.insert(0, "0")
        }

        //只能输入一个小数点
        if (edt.toString().contains(".") && edt.toString().lastIndexOf(".") != edt.toString()
                .indexOf(".")
        ) {
            edt.delete(edt.toString().length - 1, edt.toString().length)
        }

        //保留两位小数
        val posDot = edt.toString().indexOf(".")
        if (posDot <= 0) {
            return
        }
        if (edt.toString().length - posDot - 1 > 2) {
            edt.delete(posDot + 3, posDot + 4)
        }
    }
    @JvmStatic
    fun hideSoftMethod(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        @Suppress("DEPRECATION")
        imm.toggleSoftInput(0, 2)
    }
    @JvmStatic
    fun getTime(date: Date?): String {
        return if (date != null) {
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(date)
        } else {
            ""
        }
    }

    fun getYearDate(date: Date?): String {
        return if (date != null) {
            SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
        } else {
            ""
        }
    }

    fun getDayDate(date: Date?): String {
        return if (date != null) {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        } else {
            ""
        }
    }


    fun getRealMintuteDate(date: Date?): String {
        return if (date != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } else {
            ""
        }
    }

    fun getWeek(time: String?): String {
        var Week = ""
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val c = Calendar.getInstance()
        try {
            c.time = format.parse(time ?: "") ?: return ""
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val wek = c[7]
        if (wek == 1) {
            Week = Week + "星期日"
        }
        if (wek == 2) {
            Week = Week + "星期一"
        }
        if (wek == 3) {
            Week = Week + "星期二"
        }
        if (wek == 4) {
            Week = Week + "星期三"
        }
        if (wek == 5) {
            Week = Week + "星期四"
        }
        if (wek == 6) {
            Week = Week + "星期五"
        }
        return if (wek != 7) {
            Week
        } else Week + "星期六"
    }

    /**
     * @Desc 获取两个时间之间的间隔天数（方式2）
     * @param startTimeStr
     * @param endTimeStr
     * @return
     */
    fun getBetweenDays(startTimeStr: String?, endTimeStr: String?): Int {
        val betweenDays: Int
        val startTime = strToDateLong(startTimeStr)
        val endTime = strToDateLong(endTimeStr)
        val start = startTime?.time ?: 0
        val end = endTime?.time ?: 0
        betweenDays = ((end - start) / (24 * 3600 * 1000)).toInt()
        return betweenDays
    }

    internal fun strToDateLong(strDate: String?): Date? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val pos = ParsePosition(0)
        return formatter.parse(strDate ?: "", pos)
    }

    fun getSpaceTime(millisecond: Long): String {
        val spaceSecond = java.lang.Long.valueOf(
            (java.lang.Long.valueOf(Calendar.getInstance().timeInMillis)
                .toLong() - millisecond) / 1000
        )
        if (spaceSecond.toLong() >= 0 && spaceSecond.toLong() < 60) {
            return "片刻之前"
        }
        return if (spaceSecond.toLong() / 60 > 0 && spaceSecond.toLong() / 60 < 60) {
            (spaceSecond.toLong() / 60).toString() + "分钟之前"
        } else if (spaceSecond.toLong() / 3600 > 0 && spaceSecond.toLong() / 3600 < 24) {
            (spaceSecond.toLong() / 3600).toString() + "小时之前"
        } else if (spaceSecond.toLong() / 86400 >= 3) {
            getDateTimeFromMillisecond(millisecond)
        } else {
            (spaceSecond.toLong() / 86400).toString() + "天之前"
        }
    }

    /**
     * Get multilingual time display string
     */
    fun getMultilingualSpaceTime(context: Context, millisecond: Long): String {
        val spaceSecond = java.lang.Long.valueOf(
            (java.lang.Long.valueOf(Calendar.getInstance().timeInMillis)
                .toLong() - millisecond) / 1000
        )
        if (spaceSecond.toLong() >= 0 && spaceSecond.toLong() < 60) {
            return context.getString(R.string.time_moments_ago)
        }
        return if (spaceSecond.toLong() / 60 > 0 && spaceSecond.toLong() / 60 < 60) {
            context.getString(R.string.time_minutes_ago, spaceSecond.toLong() / 60)
        } else if (spaceSecond.toLong() / 3600 > 0 && spaceSecond.toLong() / 3600 < 24) {
            context.getString(R.string.time_hours_ago, spaceSecond.toLong() / 3600)
        } else {
            val days = spaceSecond.toLong() / 86400
            // Fix grammar: "1 day ago" instead of "1 days ago"
            val daysString = context.getString(R.string.time_days_ago, days)
            if (days == 1L && daysString.contains("days")) {
                daysString.replace("days", "day")
            } else {
                daysString
            }
        }
    }

    private fun getDateTimeFromMillisecond(millisecond: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millisecond))
    }
    
    /**
     * Parse backend date string and format for display as relative time
     * Supports formats:
     * - ISO format with microseconds: yyyy-MM-dd'T'HH:mm:ss:SSSSSS (e.g., 2025-11-30T02:40:38:787798)
     * - ISO format with milliseconds: yyyy-MM-dd'T'HH:mm:ss.SSS
     * - Standard format: yyyy-MM-dd HH:mm:ss
     * Returns relative time (moments ago, minutes ago, hours ago, days ago) for all dates
     */
    fun formatBackendDate(context: Context, backendDate: String?): String {
        if (backendDate.isNullOrEmpty()) return ""
        
        return try {
            val date = parseBackendDate(backendDate) ?: return context.getString(R.string.time_unknown)
            
            // Calculate time difference
            val now = Calendar.getInstance().timeInMillis
            val dateTime = date.time
            val diffSeconds = (now - dateTime) / 1000
            
            // Handle future dates (shouldn't happen, but just in case)
            if (diffSeconds < 0) {
                return context.getString(R.string.time_moments_ago)
            }
            
            when {
                diffSeconds < 60 -> context.getString(R.string.time_moments_ago)
                diffSeconds < 3600 -> context.getString(R.string.time_minutes_ago, diffSeconds / 60)
                diffSeconds < 86400 -> context.getString(R.string.time_hours_ago, diffSeconds / 3600)
                else -> {
                    val days = diffSeconds / 86400
                    // Fix grammar: "1 day ago" instead of "1 days ago"
                    val daysString = context.getString(R.string.time_days_ago, days)
                    if (days == 1L && daysString.contains("days")) {
                        daysString.replace("days", "day")
                    } else {
                        daysString
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            context.getString(R.string.time_unknown)
        }
    }
    
    /**
     * Parse backend date string supporting multiple formats
     * Supports:
     * - ISO format with microseconds: 2025-11-30T02:40:38:787798
     * - ISO format with milliseconds: 2025-11-30T02:40:38.787
     * - Standard format: 2025-11-30 02:40:38
     * - Date only: 2025-11-30
     * 
     * Note: ISO format dates (with 'T' separator) are assumed to be in UTC timezone
     * per API documentation. Standard format dates use device's local timezone.
     */
    fun parseBackendDate(backendDate: String): Date? {
        // Clean up the date string - handle microseconds by truncating to milliseconds
        var cleanDate = backendDate.trim()
        val isIsoFormat = cleanDate.contains("T")
        
        // Handle ISO format with 'T' separator: 2025-11-30T02:40:38:787798
        // Replace the last colon before microseconds with a dot for standard parsing
        if (isIsoFormat) {
            // Pattern: yyyy-MM-ddTHH:mm:ss:SSSSSS or yyyy-MM-ddTHH:mm:ss.SSSSSS
            val regex = Regex("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})[:.](\\d+)")
            val match = regex.find(cleanDate)
            if (match != null) {
                val dateTimePart = match.groupValues[1]
                val fracSeconds = match.groupValues[2]
                // Take only first 3 digits for milliseconds
                val millis = fracSeconds.take(3).padEnd(3, '0')
                cleanDate = "$dateTimePart.$millis"
            }
        }
        
        // Try different date formats
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        
        for (format in formats) {
            try {
                val formatter = SimpleDateFormat(format, Locale.US)
                // ISO format dates (with 'T') are assumed to be in UTC per API documentation
                if (isIsoFormat && format.contains("'T'")) {
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                }
                return formatter.parse(cleanDate)
            } catch (e: Exception) {
                // Try next format
            }
        }
        
        return null
    }
    
    /**
     * Format backend date for saved articles / reading history display
     * Returns HTML-formatted string like "<b>December 21st, 2025</b> 14:00"
     */
    fun formatBackendDateWithTime(backendDate: String?): String {
        if (backendDate.isNullOrEmpty()) return ""
        
        return try {
            val date = parseBackendDate(backendDate) ?: return backendDate
            
            val calendar = Calendar.getInstance().apply { time = date }
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            // Get ordinal suffix
            val ordinalSuffix = when (day) {
                1, 21, 31 -> "st"
                2, 22 -> "nd"
                3, 23 -> "rd"
                else -> "th"
            }
            
            // Format month and day
            val monthDayFormatter = SimpleDateFormat("MMMM d", Locale.getDefault())
            val monthDay = monthDayFormatter.format(date)
            
            // Format year
            val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())
            val year = yearFormatter.format(date)
            
            // Format time as 24-hour
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = timeFormatter.format(date)
            
            "<b>$monthDay$ordinalSuffix, $year</b> $time"
        } catch (e: Exception) {
            e.printStackTrace()
            backendDate
        }
    }

    fun getFormatTime(date: Date?): String {
        return if (date != null) {
            SimpleDateFormat("yyyy年MM月", Locale.getDefault()).format(date)
        } else {
            ""
        }
    }

    fun get_coordinate(latitude: Double, longitude: Double, altitude: Double): DoubleArray {
        val B = Math.toRadians(latitude)
        val L = Math.toRadians(longitude)
        val x: Double
        val y: Double
        val z: Double
        val f = 1 / 298.257223563
        val r = 6378137.0
        r * (1 - f)
        val e = Math.sqrt(2 * f - f * f)
        val N = r / Math.sqrt(1 - e * e * Math.sin(B) * Math.sin(B))
        x = (N + altitude) * Math.cos(B) * Math.cos(L)
        y = (N + altitude) * Math.cos(B) * Math.sin(L)
        z = (N * (1 - e * e) + altitude) * Math.sin(B)
        return doubleArrayOf(x, y, z)
    }
    @JvmStatic
    fun getSecondTime(date: Date?): String {
        return if (date != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        } else {
            ""
        }
    }

    fun isDateOneBigger(str1: String?, str2: String?): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var dt1: Date? = null
        var dt2: Date? = null
        try {
            dt1 = sdf.parse(str1 ?: "")
            dt2 = sdf.parse(str2 ?: "")
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        if (dt1!!.time > dt2!!.time) {
            return true
        }
        return if (dt1.time < dt2.time) {
            false
        } else false
    }

    fun getJson(context: Context, fileName: String?): String {
        val stringBuilder = StringBuilder()
        try {
            val bf = BufferedReader(
                InputStreamReader(
                    context.assets.open(
                        fileName!!
                    )
                )
            )
            while (true) {
                val line = bf.readLine() ?: break
                stringBuilder.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuilder.toString()
    }

    fun getVersionName(mContext: Context): String {
        return try {
            mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }


    fun getVersionCode(mContext: Context): Long {
        try {
            val packageInfo = mContext.packageManager.getPackageInfo(mContext.packageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return 0L
        }
    }

    fun hidePhone4Number(phone: String): String {
        return phone.replace("(\\d{3})\\d{4}(\\d{4})".toRegex(), "$1****$2")
    }

    fun hideCardLast4Number(card: String): String {
        return card.replace("(\\d{14}\\d{4})".toRegex(), "$1****")
    }

    fun hideAli(ali: String): String {
        return ali.replace("(\\d{4})\\d{9}(\\d{3})".toRegex(), "$1****$2")
    }

    /**
     * Check if the user is currently logged in
     * @param context The application context
     * @return true if user is logged in, false otherwise
     */
    fun isUserLoggedIn(context: Context): Boolean {
        return SharedPreferenceUtils.isUserLoggedIn(context)
    }

    fun toWeb(url: String?, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

    val month: Int
        get() = Calendar.getInstance()[2] + 1

    fun toSelfSetting(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, 
            Uri.fromParts("package", context.packageName, null))
        context.startActivity(intent)
    }

    fun isLogin(context: Context?): Boolean {
        return SharedPreferenceUtils.getBoolean(context!!, "autoLogin")
    }
    @JvmStatic
    fun createJsonRequestBody(json: String): RequestBody {
        return json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    }

    /**
     * 判断网络定位是否打开
     *
     * @return 定位打开结果
     */
    fun isLocationEnable(activity: Activity): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }
    fun dateDiff(startTime: String?, endTime: String?, format: String?): Long {
        // 按照传入的格式生成一个simpledateformate对象
        val sd = SimpleDateFormat(format, Locale.getDefault())
        val nd = (1000 * 24 * 60 * 60).toLong() // 一天的毫秒数
        val nh = (1000 * 60 * 60).toLong() // 一小时的毫秒数
        val nm = (1000 * 60).toLong() // 一分钟的毫秒数
        val ns: Long = 1000 // 一秒钟的毫秒数
        val diff: Long
        var day: Long = 0
        try {
            // 获得两个时间的毫秒时间差异
            diff = ((sd.parse(endTime ?: "")?.time ?: 0)
                    - (sd.parse(startTime ?: "")?.time ?: 0))
            day = diff / nd // 计算差多少天
            val hour = diff % nd / nh // 计算差多少小时
            val min = diff % nd % nh / nm // 计算差多少分钟
            val sec = diff % nd % nh % nm / ns // 计算差多少秒
            // 输出结果
            println(
                "时间相差：" + day + "天" + hour + "小时" + min
                        + "分钟" + sec + "秒。"
            )
            return if (day >= 1) {
                day
            } else {
                if (day == 0L) {
                    1
                } else {
                    0
                }
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return 0
    }

    fun formatBank(str: String): String {
        val sb = StringBuilder(str)
        val length = str.length / 4 + str.length
        for (i in 0 until length) {
            if (i % 5 == 0) {
                sb.insert(i, " ")
            }
        }
        return sb.deleteCharAt(0).toString()
    }
}