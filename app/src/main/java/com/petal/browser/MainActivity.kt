package com.petal.browser

import android.app.*
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface
import android.content.*
import android.text.InputType
import org.json.JSONArray
import kotlin.random.Random

class BrowserTab(val web: WebView, var title: String = "Новая вкладка", var url: String = "https://www.google.com")

class MainActivity : Activity() {
    private val bg = Color.rgb(19,19,26)
    private val side = Color.rgb(17,17,23)
    private val card = Color.rgb(27,27,37)
    private val field = Color.rgb(34,34,46)
    private val pink = Color.rgb(236,72,153)
    private val purple = Color.rgb(124,92,255)
    private val white = Color.rgb(232,234,237)
    private val muted = Color.rgb(154,160,166)
    private lateinit var root: LinearLayout
    private lateinit var browserHost: FrameLayout
    private lateinit var address: EditText
    private lateinit var tabsButton: TextView
    private val tabs = mutableListOf<BrowserTab>()
    private var current = 0
    private val history = mutableListOf<String>()
    private val prefs by lazy { getSharedPreferences("petal", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = side
        window.navigationBarColor = side
        buildShell()
        try {
            val saved = JSONArray(prefs.getString("history", "[]") ?: "[]")
            for (i in 0 until saved.length()) history.add(saved.getString(i))
        } catch (_: Exception) {}
        addTab("https://www.google.com")
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(s:String, size:Float=14f, color:Int=white, bold:Boolean=false): TextView =
        TextView(this).apply { text=s; textSize=size; setTextColor(color); if(bold) setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER_VERTICAL }
    private fun button(s:String, action:()->Unit): TextView = text(s,18f).apply {
        gravity=Gravity.CENTER; setPadding(dp(12),dp(8),dp(12),dp(8))
        setOnClickListener { action() }
        background = rounded(card,12)
    }
    private fun rounded(color:Int, radius:Int): android.graphics.drawable.Drawable =
        android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat() }

    private fun buildShell() {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(10),dp(7),dp(10),dp(7)); setBackgroundColor(side) }
        top.addView(text("Petal",20f,white,true), LinearLayout.LayoutParams(0,dp(42),1f))
        tabsButton=text("▢ 1",14f).apply { gravity=Gravity.CENTER; setPadding(dp(10),dp(8),dp(10),dp(8)); setOnClickListener { showTabs() } }
        top.addView(tabsButton)
        top.addView(button("＋") { addTab("https://www.google.com") }, LinearLayout.LayoutParams(dp(44),dp(42)))
        root.addView(top)
        val bar=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(9),dp(6),dp(9),dp(6)); setBackgroundColor(side) }
        bar.addView(button("‹"){ goBack() }, LinearLayout.LayoutParams(dp(42),dp(42)))
        address=EditText(this).apply {
            singleLine=true; textSize=14f; setTextColor(white); setHintTextColor(muted); hint="Поиск или адрес сайта"
            inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            imeOptions=EditorInfo.IME_ACTION_GO; setPadding(dp(12),0,dp(12),0); background=rounded(field,13)
            setOnEditorActionListener { _, action, _ -> if(action==EditorInfo.IME_ACTION_GO){ navigate(text.toString()); true } else false }
        }
        bar.addView(address,LinearLayout.LayoutParams(0,dp(42),1f).apply { setMargins(dp(7),0,dp(7),0) })
        bar.addView(button("↻"){ tabs.getOrNull(current)?.web?.reload() },LinearLayout.LayoutParams(dp(42),dp(42)))
        root.addView(bar)
        browserHost=FrameLayout(this)
        root.addView(browserHost,LinearLayout.LayoutParams(-1,0,1f))
        val bottom=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; setPadding(dp(5),dp(5),dp(5),dp(5)); setBackgroundColor(side) }
        listOf("⌂" to { home() }, "♫" to { showTool("Саундпад") }, "＋" to { addTab("https://www.google.com") }, "⋯" to { showMenu() }).forEach { (label,act) ->
            bottom.addView(text(label,21f).apply { gravity=Gravity.CENTER; setOnClickListener { act() } },LinearLayout.LayoutParams(0,dp(48),1f))
        }
        root.addView(bottom)
        setContentView(root)
    }

    private fun addTab(url:String) {
        val w=WebView(this)
        w.settings.javaScriptEnabled=true
        w.settings.domStorageEnabled=true
        w.settings.loadsImagesAutomatically=true
        w.settings.useWideViewPort=true
        w.settings.loadWithOverviewMode=true
        w.settings.setSupportZoom(true)
        w.webViewClient=object: WebViewClient(){
            override fun onPageFinished(view:WebView?, u:String?) {
                if(current<tabs.size && tabs[current].web===view) {
                    address.setText(u ?: "")
                    tabs[current].url=u ?: ""
                    tabs[current].title=view?.title ?: "Petal"
                    if(u!=null && !u.startsWith("about:")) { history.add(u); saveHistory() }
                }
            }
        }
        w.webChromeClient=WebChromeClient()
        tabs.add(BrowserTab(w))
        current=tabs.lastIndex
        browserHost.removeAllViews()
        browserHost.addView(w,FrameLayout.LayoutParams(-1,-1))
        tabsButton.text="▢ ${tabs.size}"
        w.loadUrl(url)
    }
    private fun navigate(raw:String) {
        var u=raw.trim()
        if(u.isEmpty()) return
        if(!u.contains(" ") && (u.startsWith("https://")||u.startsWith("http://")||u.contains("."))) {
            if(!u.startsWith("http")) u="https://$u"
        } else u="https://www.google.com/search?q="+java.net.URLEncoder.encode(u,"UTF-8")
        tabs.getOrNull(current)?.web?.loadUrl(u)
    }
    private fun switchTab(i:Int) {
        if(i !in tabs.indices) return
        current=i; browserHost.removeAllViews()
        browserHost.addView(tabs[i].web,FrameLayout.LayoutParams(-1,-1))
        address.setText(tabs[i].web.url ?: "")
        tabsButton.text="▢ ${tabs.size}"
    }
    private fun showTabs() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)); setBackgroundColor(bg) }
        box.addView(text("Вкладки",22f,white,true))
        tabs.forEachIndexed { i,t ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(8),0,dp(8)) }
            row.addView(text("${i+1}. ${t.title}",15f),LinearLayout.LayoutParams(0,dp(48),1f))
            row.addView(button("Открыть"){ switchTab(i); (box.parent as? android.view.ViewGroup)?.let{}; dialog.dismiss() })
            row.addView(button("×"){ if(tabs.size>1){ browserHost.removeView(t.web); t.web.destroy(); tabs.removeAt(i); if(current>=tabs.size) current=tabs.lastIndex; switchTab(current); dialog.dismiss(); showTabs() } })
            box.addView(row)
        }
        box.addView(button("＋ Новая вкладка"){ dialog.dismiss(); addTab("https://www.google.com") })
        dialog=AlertDialog.Builder(this).setView(box).create()
        dialog.show()
    }
    private lateinit var dialog: AlertDialog
    private fun showMenu() {
        val items=arrayOf("История","Калькулятор","Случайные числа","Колесо удачи","Настройки","Загрузки")
        AlertDialog.Builder(this).setTitle("Petal").setItems(items){ _,which -> showTool(items[which]) }.show()
    }
    private fun showTool(name:String) {
        val panel=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(18),dp(20),dp(18)); setBackgroundColor(bg) }
        panel.addView(text(name,22f,white,true))
        when(name) {
            "Калькулятор" -> {
                val input=EditText(this).apply { hint="Например: 12 * (3 + 4)"; setTextColor(white); setHintTextColor(muted); background=rounded(field,12); setPadding(dp(12),dp(10),dp(12),dp(10)) }
                val result=text("Результат появится здесь",16f,pink)
                panel.addView(input); panel.addView(button("Вычислить"){ try { result.text=calculate(input.text.toString()) } catch(_:Exception){ result.text="Не удалось вычислить" } }); panel.addView(result)
            }
            "Случайные числа" -> {
                val lo=EditText(this).apply { hint="От"; inputType=2; setTextColor(white); background=rounded(field,10) }
                val hi=EditText(this).apply { hint="До"; inputType=2; setTextColor(white); background=rounded(field,10) }
                val result=text("—",20f,pink,true)
                panel.addView(lo); panel.addView(hi); panel.addView(button("Сгенерировать"){ val a=lo.text.toString().toIntOrNull()?:0; val b=hi.text.toString().toIntOrNull()?:100; result.text=Random.nextInt(minOf(a,b),maxOf(a,b)+1).toString() }); panel.addView(result)
            }
            "Колесо удачи" -> {
                val opts=EditText(this).apply { hint="Варианты через запятую"; setTextColor(white); background=rounded(field,10) }
                val result=text("Крути колесо!",18f,pink,true)
                panel.addView(opts); panel.addView(button("Крутить"){ val a=opts.text.toString().split(",").map{it.trim()}.filter{it.isNotEmpty()}; result.text=if(a.isEmpty()) "Добавь варианты" else "Выпало: "+a.random() }); panel.addView(result)
            }
            "История" -> {
                history.takeLast(100).reversed().forEach { panel.addView(text("• $it",13f)) }
                panel.addView(button("Очистить историю"){ history.clear(); saveHistory(); panel.removeAllViews(); panel.addView(text("История очищена",15f)) })
            }
            "Саундпад" -> panel.addView(text("Мобильный саундпад будет добавлен отдельно. В этой версии доступны браузер и основные инструменты.",14f,muted))
            "Настройки" -> panel.addView(text("Petal Mobile · тёмная тема · версия 1.0",14f,muted))
            "Загрузки" -> panel.addView(text("Загрузки выполняются средствами Android WebView.",14f,muted))
        }
        AlertDialog.Builder(this).setView(panel).setPositiveButton("Закрыть",null).show()
    }
    private fun calculate(expr:String):String {
        val tokens=Regex("\\d+(?:\\.\\d+)?|[()+*/-]").findAll(expr).map{it.value}.toList()
        var p=0
        fun atom():Double { val t=tokens[p++]; return when(t) { "(" -> exprSum().also { if(tokens[p++]!=")") error(")") }; else -> t.toDouble() } }
        fun term():Double { var v=atom(); while(p<tokens.size && tokens[p] in listOf("*","/")) { val op=tokens[p++]; val b=atom(); v=if(op=="*")v*b else v/b }; return v }
        fun exprSum():Double { var v=term(); while(p<tokens.size && tokens[p] in listOf("+","-")) { val op=tokens[p++]; val b=term(); v=if(op=="+")v+b else v-b }; return v }
        val v=exprSum(); require(p==tokens.size); return v.toString()
    }
    private fun saveHistory(){ prefs.edit().putString("history",JSONArray(history).toString()).apply() }
    private fun goBack(){ val w=tabs.getOrNull(current)?.web ?: return; if(w.canGoBack()) w.goBack() }
    private fun home(){ tabs.getOrNull(current)?.web?.loadUrl("https://www.google.com") }
    override fun onBackPressed(){ val w=tabs.getOrNull(current)?.web; if(w?.canGoBack()==true) w.goBack() else super.onBackPressed() }
}
