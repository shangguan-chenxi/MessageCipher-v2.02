package chenxi.shangguan.messageciher_v202

import android.content.ClipData
import android.content.ClipboardManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messageciher_v202.databinding.ActivityAesCipherBinding

class AesCipher : AppCompatActivity() {
    private lateinit var ui : ActivityAesCipherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_aes_cipher)
        ui = ActivityAesCipherBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 密码库
        val secLib = SecurityLib()

        ui.aesPwd.setText(intent.getStringExtra(AES_PWD_PASS))

        ui.btnEncrypt.setOnClickListener {
            val plainText = ui.plainText.text.toString()
            val pwd = ui.aesPwd.text.toString()

            if (plainText == ""){
                AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else if(pwd == ""){
                AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密码").setPositiveButton("好" , null ).create().show()
            }else if (pwd.length == 16 || pwd.length == 24 || pwd.length == 32){
                val encryptedText = secLib.aesEncrypt(plainText, pwd)
                if (encryptedText == null) {
                    AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("发生未知错误").setPositiveButton("好" , null ).create().show()
                }else{
                    ui.plainText.text.clear()
                    ui.encryptedText.text.clear()
                    ui.encryptedText.setText(encryptedText)

                    // 将内容复制到剪贴板
                    try {
                        //获取剪贴板管理器
                        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        // 创建普通字符型ClipData
                        val mClipData = ClipData.newPlainText("Label", ui.encryptedText.text)
                        // 将ClipData内容放到系统剪贴板里。
                        cm.setPrimaryClip(mClipData);

                    } catch (e: Exception) {

                    }
                }
            }else{
                AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
            }
        }

        ui.btnDecrypt.setOnClickListener {
            val encryptedText = ui.encryptedText.text.toString()
            val pwd = ui.aesPwd.text.toString()

            if (encryptedText == ""){
                AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else if(pwd == ""){
                AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("需要提供密码").setPositiveButton("好" , null ).create().show()
            }else if (pwd.length == 16 || pwd.length == 24 || pwd.length == 32){
                val decryptedText = secLib.aesDecrypt(encryptedText, pwd)
                if (decryptedText == null){
                    AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("发生未知错误").setPositiveButton("好" , null ).create().show()
                }else{
                    ui.plainText.text.clear()
                    ui.plainText.setText(decryptedText)
                }
            }else{
                AlertDialog.Builder(this@AesCipher).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
            }
        }

        ui.btnDelPlainTxt.setOnClickListener {
            ui.plainText.text.clear()
        }

        ui.btnPurgeAll.setOnClickListener {
            ui.plainText.text.clear()
            ui.aesPwd.text.clear()
            ui.encryptedText.text.clear()

            // 清空剪贴板
            try{
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val mClipData = ClipData.newPlainText("", "")
                cm.setPrimaryClip(mClipData)
            }catch (e: Exception){

            }

            AlertDialog.Builder(this@AesCipher).setTitle("提示").setMessage("重置成功").setPositiveButton("好" , null ).create().show()
        }

        ui.butDelEncryptedTxt.setOnClickListener {
            ui.encryptedText.text.clear()
        }

        ui.btnPasteEncryptedTxt.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.encryptedText.text.clear()
                    ui.encryptedText.setText(pasteString)

                    ui.btnDecrypt.callOnClick()
                }
            }catch (e: Exception){

            }
        }
    }
}