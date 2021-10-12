@file:Suppress("DEPRECATION")

package chenxi.shangguan.messageciher_v202

import android.app.ActivityManager
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messageciher_v202.databinding.ActivityChangeAuthCodeBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.system.exitProcess

class ChangeAuthCode : AppCompatActivity() {

    private lateinit var ui: ActivityChangeAuthCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        ui = ActivityChangeAuthCodeBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        // 密码库
        val secLib = SecurityLib()

        // 先看看文件在不在，文件有就开始验证，没有就提示要创建口令
        val authFile = File(filesDir, secLib.authFileName) // 内部储存

        // 获取MAC
        val mac: String? = Utils().getMACAddress("wlan0")

        /**
         * 修改密码，旧密码解密然后新密码加密 验证文件的内容
         * */
        ui.btnChange.setOnClickListener {
            val oldCode = ui.txtOldAuthCode.text.toString()
            val newCode = ui.txtNewAuthCode.text.toString()
            val newReCode = ui.txtReNewAuthCode.text.toString()
            if (oldCode == ""){
                ui.prompt.setText("'原口令' 不能为空")
            }else if(newCode == ""){
                ui.prompt.setText("'新口令' 不能为空")
            }else if(newReCode == ""){
                ui.prompt.setText("'校验新口令' 不能为空")
            }else if(newCode != newReCode){
                ui.prompt.setText("'新口令' 和 '校验新口令' 不匹配")
            }else{
                val fos = FileInputStream(authFile)
                val byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
                val len = fos.read(byte)
                val readContent = String(byte, 0, len)
                val sectors = readContent.split(".")

                val originalStr = mac + oldCode
                var thisAesKey = secLib.getStringMD5(originalStr)

                if (secLib.CHECK_AUTH_STR == secLib.aesDecrypt(sectors[0], thisAesKey!!) && sectors.size == 2){
                    val newStr = mac + newCode
                    val newAuthKey = secLib.getStringMD5(newStr)
                    val MasterAesKey = secLib.aesDecrypt(sectors[1], thisAesKey)
                    val newAuthString = "${secLib.aesEncrypt(secLib.CHECK_AUTH_STR, newAuthKey!!)}.${secLib.aesEncrypt(MasterAesKey!!, newAuthKey)}"

                    val f = FileOutputStream(authFile)
                    f.write(newAuthString.toByteArray())
                    f.close()

                    AlertDialog.Builder(this).setTitle("提示").setMessage("口令修改成功").setPositiveButton("好", DialogInterface.OnClickListener { dialogInterface, i ->
                        ExitApplication.instance!!.exit()
                    }).setOnDismissListener{
                        ExitApplication.instance!!.exit()
                    }.create().show()
                    /*
                    // Dalvik VM的本地方法，不太行，退了不能回到验证界面
                    android.os.Process.killProcess(android.os.Process.myPid())    //获取PID
                    exitProcess(0)
                     */

                    /*
                    // 任务管理器方法，需要以下权限
                    //<uses-permission android:name=\"android.permission.RESTART_PACKAGES\"></uses-permission>
                    ActivityManager am = (ActivityManager)getSystemService (Context.ACTIVITY_SERVICE);
                    am.restartPackage(getPackageName());

                    val am : ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager;
                        am.restartPackage(packageName);
                    * */
                }else{
                    ui.prompt.setText("错误的原口令")
                    ui.txtOldAuthCode.setText("")
                }
            }

        }

        /**
         * 清空
         * */
        ui.btnClear.setOnClickListener {
            ui.txtOldAuthCode.text.clear()
            ui.txtNewAuthCode.text.clear()
            ui.txtReNewAuthCode.text.clear()
            ui.prompt.setText("已清空")
        }

        /**
         * 退出程序
         * */
        ui.btnExit2.setOnClickListener {
            ExitApplication.instance?.exit()
        }
    }
}