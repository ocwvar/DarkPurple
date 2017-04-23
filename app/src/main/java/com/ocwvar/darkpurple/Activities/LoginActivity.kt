package com.ocwvar.darkpurple.Activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatCheckBox
import android.support.v7.widget.AppCompatEditText
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import com.ocwvar.darkpurple.AppConfigs
import com.ocwvar.darkpurple.Callbacks.NetworkCallbacks.LoginUI.OnLoginCallbacks
import com.ocwvar.darkpurple.Network.APIs
import com.ocwvar.darkpurple.Network.Keys
import com.ocwvar.darkpurple.Network.NetworkRequest
import com.ocwvar.darkpurple.Network.NetworkRequestTypes
import com.ocwvar.darkpurple.R
import com.ocwvar.darkpurple.Units.BaseActivity
import org.jetbrains.annotations.TestOnly

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/03/16 6:03 PM
 * File Location com.ocwvar.darkpurple.Activities
 * This file use to :   登录页面
 */
class LoginActivity : BaseActivity(), OnLoginCallbacks {

    private lateinit var inputUsername: AppCompatEditText
    private lateinit var inputPassword: AppCompatEditText
    private lateinit var rememberButton: AppCompatCheckBox
    private lateinit var loginRegist: View
    private lateinit var loginOffLine: View
    private lateinit var login: View

    override fun onPreSetup(): Boolean {
        AppConfigs.USER.TOKEN = ""
        AppConfigs.USER.USERNAME = ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        return true
    }

    override fun setActivityView(): Int {
        return R.layout.activity_login
    }

    override fun onSetToolBar(): Int {
        return 0
    }

    override fun onSetupViews() {
        inputUsername = findViewById(R.id.login_input_username) as AppCompatEditText
        inputPassword = findViewById(R.id.login_input_password) as AppCompatEditText
        rememberButton = findViewById(R.id.login_remember) as AppCompatCheckBox

        loginRegist = findViewById(R.id.login_start_register)
        loginOffLine = findViewById(R.id.login_start_offline)
        login = findViewById(R.id.login_start_login)

        loginRegist.setOnClickListener(this@LoginActivity)
        loginOffLine.setOnClickListener(this@LoginActivity)
        login.setOnClickListener(this@LoginActivity)
        login.setOnLongClickListener(this@LoginActivity)

        rememberButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                //保存密码复选框被取消勾选则马上清除本地保存的账密
                clearSP()
            }
        }

        loadRememberedPW()
    }

    override fun onViewClick(clickedView: View) {
        when (clickedView.id) {
            R.id.login_start_login -> {
                //登录账号
                loginAction(true)
            }

            R.id.login_start_register -> {
                //注册账户并登录
                loginAction(false)
            }

            R.id.login_start_offline -> {
                //离线使用 , 则直接进入主界面即可
                startActivity(Intent(this@LoginActivity, SelectMusicActivity::class.java))
                finish()
            }
        }
    }

    override fun onViewLongClick(holdedView: View): Boolean {
        when (holdedView.id) {
            R.id.login_start_login -> {
                //重新设置BASE_URL
                setBaseURL()
            }
        }
        return true
    }

    /**
     * 获取请求头 , 如果输入字符串非法则显示错误信息
     * @return  可用的请求头 , 为NULL则表明请求的参数不正确
     */
    private fun getRequestHeaders(): HashMap<String, String>? {
        val username: String = inputUsername.text.toString()
        val password: String = inputPassword.text.toString()
        if (TextUtils.isEmpty(username)) {
            //用户名输入为空
            inputUsername.error = AppConfigs.ApplicationContext.getString(R.string.login_input_empty)
            return null
        } else if (TextUtils.isEmpty(password)) {
            //密码输入为空
            inputPassword.error = AppConfigs.ApplicationContext.getString(R.string.login_input_empty)
            return null
        } else if (username.length < 6) {
            //用户名输入太短
            inputUsername.error = AppConfigs.ApplicationContext.getString(R.string.login_input_too_short)
            return null
        } else if (password.length < 6) {
            //密码输入太短
            inputPassword.error = AppConfigs.ApplicationContext.getString(R.string.login_input_too_short)
            return null
        } else if (username.contains(Regex("[^\\x00-\\xff]"))) {
            //如果包含任何双字节文字
            inputUsername.error = AppConfigs.ApplicationContext.getString(R.string.login_input_only_word)
            return null
        } else {
            val headers: HashMap<String, String> = HashMap()
            headers.put(Keys.Username, username)
            headers.put(Keys.Password, password)
            return headers
        }
    }

    /**
     * 执行登录或者注册请求
     * @param   isLogin 是否是登录. False则为注册
     */
    private fun loginAction(isLogin: Boolean) {
        getRequestHeaders()?.let {
            disableAllViews()
            showHoldingSnackBar(AppConfigs.ApplicationContext.getString(R.string.simple_loading))
            val args = HashMap<String, Any>()
            args.put("args", it)
            args.put("isLogin", isLogin)
            NetworkRequest.newRequest(NetworkRequestTypes.登录或注册, args, this@LoginActivity)
        }
    }

    /**
     * 储存账户密码到本地数据中
     */
    private fun savePW2SP() {
        val sp: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity).edit()
        sp.putString(Keys.Username, inputUsername.text.toString())
        sp.putString(Keys.Password, inputPassword.text.toString())
        sp.commit()
    }

    /**
     * 清除本地保存的记录
     */
    private fun clearSP() {
        val sp: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity).edit()
        sp.remove(Keys.Username)
        sp.remove(Keys.Password)
        sp.commit()
    }

    /**
     * 读取本地保存的账密数据
     * @return  是否成功读取保存的数据
     */
    private fun loadRememberedPW() {
        val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
        if (sp.contains(Keys.Username) && sp.contains(Keys.Password)) {
            inputUsername.setText(sp.getString(Keys.Username, null))
            inputPassword.setText(sp.getString(Keys.Password, null))
            rememberButton.isChecked = true
            if (!intent.hasExtra("stopAutoLogin")) {
                loginAction(true)
            }
        }
    }

    /**
     * 禁用所有控件的使用
     */
    private fun disableAllViews() {
        inputPassword.isEnabled = false
        inputUsername.isEnabled = false
        login.isEnabled = false
        loginRegist.isEnabled = false
        loginOffLine.isEnabled = false
        rememberButton.isEnabled = false

        inputPassword.alpha = 0.5f
        inputUsername.alpha = 0.5f
        login.alpha = 0.5f
        loginRegist.alpha = 0.5f
        loginOffLine.alpha = 0.5f
        rememberButton.alpha = 0.5f
    }

    /**
     * 启用所有控件
     */
    private fun enableAllView() {
        inputPassword.isEnabled = true
        inputUsername.isEnabled = true
        login.isEnabled = true
        loginRegist.isEnabled = true
        loginOffLine.isEnabled = true
        rememberButton.isEnabled = true

        inputPassword.alpha = 1.0f
        inputUsername.alpha = 1.0f
        login.alpha = 1.0f
        loginRegist.alpha = 1.0f
        loginOffLine.alpha = 1.0f
        rememberButton.alpha = 1.0f
    }

    /**
     * 测试代码：设置BaseURL
     * 显示一个对话框用于设置BaseURL
     */
    @TestOnly
    private fun setBaseURL() {
        val input: EditText = EditText(this@LoginActivity)
        input.setText(APIs.baseURL)
        val dialog: AlertDialog = AlertDialog.Builder(this@LoginActivity)
                .setView(input)
                .setPositiveButton(R.string.simple_done, { dialog, _ ->
                    val string: String = input.text.toString()
                    if (!TextUtils.isEmpty(string) && string.last() == '/') {
                        APIs.baseURL = string
                        dialog.dismiss()
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "格式不正确，必须长度大于0，同时以\"/\"结尾", Snackbar.LENGTH_LONG).show()
                    }
                })
                .create()
        dialog.show()
    }

    /**
     * 登录成功
     * @param   username    用户名
     */
    override fun onLoginSuccess(username: String) {
        dismissHoldingSnackBar()
        enableAllView()
        if (rememberButton.isChecked) {
            savePW2SP()
        } else {
            inputUsername.text.clear()
            inputPassword.text.clear()
        }
        Toast.makeText(this@LoginActivity, String.format("%s%s", AppConfigs.ApplicationContext.getText(R.string.login_head_login), username), Toast.LENGTH_SHORT).show()
        startActivity(Intent(this@LoginActivity, SelectMusicActivity::class.java))
        finish()
    }

    /**
     * 注册成功
     * @param   username    用户名
     */
    override fun onRegisterSuccess(username: String) {
        dismissHoldingSnackBar()
        enableAllView()
        if (rememberButton.isChecked) {
            savePW2SP()
        } else {
            inputUsername.text.clear()
            inputPassword.text.clear()
        }
        Toast.makeText(this@LoginActivity, String.format("%s%s", AppConfigs.ApplicationContext.getText(R.string.login_head_register), username), Toast.LENGTH_SHORT).show()
        startActivity(Intent(this@LoginActivity, SelectMusicActivity::class.java))
        finish()
    }

    /**
     * 处理异常
     * @param   message    异常信息
     */
    override fun onError(message: String) {
        dismissHoldingSnackBar()
        enableAllView()
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

}