package chenxi.shangguan.messageciher_v202

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messageciher_v202.databinding.ActivityMainOldBinding

val AES_PWD_PASS : String = ""

class MainActivity_old : AppCompatActivity() {

    private lateinit var ui : ActivityMainOldBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main_old)
        ui = ActivityMainOldBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 密码库
        val secLib = SecurityLib()

        var keys: Map<String, Any>?
        var v1: String?
        var v2: String?

        // 初始化PSA密钥对
        ui.btnGenKeys.setOnClickListener {
            // 生成rsa密钥对
            keys = secLib.initKey()
            v1 = secLib.getPublicKey(keys as Map<String, Any>)
            v2 = secLib.getPrivateKey(keys as Map<String, Any>)

            // 清空数字签名和密文文本框
            ui.txtEncryptedTxt.text.clear()
            ui.txtSignature.text.clear()

            // 显示到文本框
            ui.txtPublicKey.setText(v1)
            ui.txtPrivateKey.setText(v2)
        }

        // 公钥加密AES pwd
        ui.btnPublicKeyEncrypt.setOnClickListener {
            // 获取并判断AES pwd长度是否合规
            var aes_pwd = ui.txtAESPwd.text.toString()
            var public_key = ui.txtPublicKey.text.toString()

            if (aes_pwd.length != 16 && aes_pwd.length != 24 && aes_pwd.length != 32){
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
            }else{
                if (public_key != ""){
                    ui.txtEncryptedTxt.setText(secLib.encryptByPublicKey(aes_pwd, public_key))
                }else{
                    AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供公钥").setPositiveButton("好" , null ).create().show()
                }
            }
        }

        // 公钥解密 encrpyed text
        ui.btnPublicKeyDecrypt.setOnClickListener {
            var encrpyed_text = ui.txtEncryptedTxt.text.toString()
            var public_key = ui.txtPublicKey.text.toString()

            if (encrpyed_text == "") {
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else{
                if (public_key != ""){
                    ui.txtAESPwd.setText(secLib.decryptByPublicKey(encrpyed_text, public_key))
                }else{
                    AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供公钥").setPositiveButton("好" , null ).create().show()
                }
            }
        }

        // 私钥加密 AES pwd
        ui.btnPrivateKeyEncrypt.setOnClickListener {
            var aes_pwd = ui.txtAESPwd.text.toString()
            var private_key = ui.txtPrivateKey.text.toString()

            if (aes_pwd.length != 16 && aes_pwd.length != 24 && aes_pwd.length != 32){
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
            }else{
                if (private_key != ""){
                    ui.txtEncryptedTxt.setText(secLib.encryptByPrivateKey(aes_pwd, private_key))
                }else{
                    AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供私钥").setPositiveButton("好" , null ).create().show()
                }
            }
        }

        // 私钥解密 encrpyed text
        ui.btnPrivateKeyDecrypt.setOnClickListener {
            // 获取已加密的文本和私钥
            var encrypted_text = ui.txtEncryptedTxt.text.toString()
            var private_key = ui.txtPrivateKey.text.toString()

            if(encrypted_text == ""){
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else if (private_key == ""){
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供私钥").setPositiveButton("好" , null ).create().show()
            }else{
                ui.txtAESPwd.setText(secLib.decryptByPrivateKey(encrypted_text, private_key))
            }
        }

        // 私钥对加密后的数据签名
        ui.btnGenSignature.setOnClickListener {
            var encrpyed_text = ui.txtEncryptedTxt.text.toString()
            var private_key = ui.txtPrivateKey.text.toString()
            var aes_pwd = ui.txtAESPwd.text.toString()

            if (private_key != "") {
                if (encrpyed_text == "") {
                    if (aes_pwd.length != 16 && aes_pwd.length != 24 && aes_pwd.length != 32) {
                        AlertDialog.Builder(this@MainActivity_old).setTitle("错误")
                                .setMessage("密码应为16/24/32字符长度").setPositiveButton("好" , null ).create().show()
                    } else {
                        ui.txtEncryptedTxt.setText(secLib.encryptByPrivateKey(aes_pwd, private_key))
                        ui.txtSignature.setText(
                                secLib.sign(
                                        ui.txtEncryptedTxt.text.toString(),
                                        private_key
                                )
                        )

                        // 将内容复制到剪贴板
                        try {
                            //获取剪贴板管理器
                            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            // 创建普通字符型ClipData
                            val mClipData = ClipData.newPlainText("Label", ui.txtSignature.text)
                            // 将ClipData内容放到系统剪贴板里。
                            cm.setPrimaryClip(mClipData);
                            AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("已完成加密和签名并已复制到剪贴板").setPositiveButton("好" , null ).create().show()
                        } catch (e: Exception) {

                        }
                    }
                }else{
                    ui.txtSignature.setText(secLib.sign(ui.txtEncryptedTxt.text.toString(), private_key))
                    AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("已完成签名并已复制到剪贴板").setPositiveButton("好" , null ).create().show()
                }
            }else{
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供私钥").setPositiveButton("好" , null ).create().show()
            }
        }

        // 用公钥和加密数据验证签名
        ui.btnVerifySignature.setOnClickListener {
            var encrpyed_text = ui.txtEncryptedTxt.text.toString()
            var public_key = ui.txtPublicKey.text.toString()
            var signature_text = ui.txtSignature.text.toString()

            if (encrpyed_text == "") {
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供密文").setPositiveButton("好" , null ).create().show()
            }else if (public_key == ""){
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供公钥").setPositiveButton("好" , null ).create().show()
            }else if (signature_text == ""){
                AlertDialog.Builder(this@MainActivity_old).setTitle("错误").setMessage("需要提供数字签名").setPositiveButton("好" , null ).create().show()
            }else{
                if (secLib.verify(encrpyed_text, public_key, signature_text)){
                    ui.txtAESPwd.setText(secLib.decryptByPublicKey(encrpyed_text, public_key))
                    AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("合法签名，验证成功").setPositiveButton("好" , null ).create().show()
                }else{
                    AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("签名不合法，验证失败").setPositiveButton("好" , null ).create().show()
                }
            }
        }


        // 清空AES密码文本框
        ui.btnClearAesPwdTxt.setOnClickListener {
            ui.txtAESPwd.text.clear()
        }

        // 清空公钥文本框
        ui.btnClearPublicKeyTxt.setOnClickListener{
            ui.txtPublicKey.text.clear()
        }

        // 清空私钥文本框
        ui.btnClearPrivateKeyTxt.setOnClickListener {
            ui.txtPrivateKey.text.clear()
        }

        // 清空数字签名文本框
        ui.btnClearSignatureTxt.setOnClickListener {
            ui.txtSignature.text.clear()
        }

        // 清空密文文本框
        ui.btnClearEncryptedTxt.setOnClickListener {
            ui.txtEncryptedTxt.text.clear()
        }

        // 复制公钥到剪贴板
        ui.btnCopyPublicKey.setOnClickListener {
            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", ui.txtPublicKey.text)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("公钥已复制到剪贴板").setPositiveButton("好" , null ).create().show()
            } catch (e: Exception) {

            }
        }

        // 从剪贴板粘贴公钥
        ui.btnPastePublicKey.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtPublicKey.text.clear()
                    ui.txtPublicKey.setText(pasteString)
                }
            }catch (e: Exception){

            }
        }

        // 复制私钥到剪贴板
        ui.btnCopyPrivateKey.setOnClickListener {
            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", ui.txtPrivateKey.text)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("私钥已复制到剪贴板").setPositiveButton("好" , null ).create().show()
            } catch (e: Exception) {

            }
        }

        // 从剪贴板粘贴私钥
        ui.btnPastePrivateKey.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtPrivateKey.text.clear()
                    ui.txtPrivateKey.setText(pasteString)
                }
            }catch (e: Exception){

            }
        }

        // 从剪贴板粘贴签名
        ui.btnPasteSignature.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtSignature.text.clear()
                    ui.txtSignature.setText(pasteString)
                }
            }catch (e: Exception){

            }
        }

        // 复制加密后的内容到剪贴板
        ui.btnCopyEncryptedTxt.setOnClickListener {
            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", ui.txtEncryptedTxt.text)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("密文已复制到剪贴板").setPositiveButton("好" , null ).create().show()
            } catch (e: Exception) {

            }
        }

        // 从剪贴板粘贴加密后的内容
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

                    ui.txtEncryptedTxt.text.clear()
                    ui.txtEncryptedTxt.setText(pasteString)
                }
            }catch (e: Exception){

            }
        }

        // 跳转到AES加解密：带上AES密码
        ui.btnGoToAES.setOnClickListener {
            val send_aes_pwd = ui.txtAESPwd.text.toString()
            ui.btnClearAesPwdTxt.callOnClick()
            ui.btnClearEncryptedTxt.callOnClick()
            val i = Intent(this, AesCipher::class.java)
            i.putExtra(AES_PWD_PASS, send_aes_pwd)
            startActivity(i)
        }

        // 清空所有文本框和剪贴板
        ui.btnPurgeAll.setOnClickListener {
            // 清空所有文本框
            ui.btnClearAesPwdTxt.callOnClick()
            ui.btnClearPublicKeyTxt.callOnClick()
            ui.btnClearPrivateKeyTxt.callOnClick()
            ui.btnClearSignatureTxt.callOnClick()
            ui.btnClearEncryptedTxt.callOnClick()

            // 清空密钥
            keys = HashMap(0)
            v1 = ""
            v2 = ""

            //清空剪贴板
            try{
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val mClipData = ClipData.newPlainText("", "")
                cm.setPrimaryClip(mClipData)
            }catch (e: Exception){

            }
            AlertDialog.Builder(this@MainActivity_old).setTitle("提示").setMessage("重置成功").setPositiveButton("好" , null ).create().show()
        }

        // 随机生成AES密码
        ui.btnGenRandAesPwd.setOnClickListener {
            // 清空数字签名和密文文本框
            ui.txtEncryptedTxt.text.clear()
            ui.txtSignature.text.clear()
            ui.txtAESPwd.setText(secLib.genAesRandom())
        }

    }
}