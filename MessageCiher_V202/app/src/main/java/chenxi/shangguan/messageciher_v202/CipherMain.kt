package chenxi.shangguan.messageciher_v202

import android.content.ClipData
import android.content.ClipboardManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messageciher_v202.databinding.ActivityCipherMainBinding
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherMain : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        var ui : ActivityCipherMainBinding = ActivityCipherMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        // 密码库
        val secLib = SecurityLib()

        // 己方RSA密钥对和对方RSA公钥
        var keys: Map<String, Any>?
        var v1: String? // 公钥
        var v2: String? // 私钥

        var contact = intent.getStringExtra("Contact")
        var urPubKey = intent.getStringExtra("UrPubKey")
        var urPriKey = intent.getStringExtra("UrPriKey")
        var oppPubKey = intent.getStringExtra("OppPubKey")

        ui.txtWho.setText(contact)

        /** 清空AES明文框 */
        ui.btnClearAesPlainText.setOnClickListener{
            ui.txtAesPlainText.text.clear()
            //AlertDialog.Builder(this).setTitle("提示").setMessage("已清空 'AES加密/解密 明文'").setPositiveButton("好", null).create().show()
            ui.txtPrompt.setText("已清空明文")
        }

        /** 清空AES密文框 */
        ui.btnClearAesCryptedText.setOnClickListener {
            ui.txtAesCryptedText.text.clear()
            //AlertDialog.Builder(this).setTitle("提示").setMessage("已清空 'AES加密/解密 密文'").setPositiveButton("好", null).create().show()
            ui.txtPrompt.setText("已清空密文")
        }

        /** 清空所有框框 */
        ui.btnPurge.setOnClickListener {
            ui.txtAesPlainText.text.clear()
            ui.txtAesCryptedText.text.clear()
            //AlertDialog.Builder(this).setTitle("提示").setMessage("重置成功").setPositiveButton("好", null).create().show()
            ui.txtPrompt.setText("已清空明文和密文")
        }

        /** 退出程序 */
        ui.btnExit.setOnClickListener {
            ExitApplication.instance!!.exit()
        }

        /** 加密明文并复制到剪贴板 */
        ui.btnAesEncryptAndCopy.setOnClickListener {
            val aesPlainText = ui.txtAesPlainText.text.toString()

/*
            if(urPubKey == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要 '己方RSA公钥'").setPositiveButton("好", null).create().show()
                return@setOnClickListener
            }

            if(urPriKey == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要 '己方RSA私钥'").setPositiveButton("好", null).create().show()
                return@setOnClickListener
            }

            if (oppPubKey == ""){
                AlertDialog.Builder(this).setTitle("错误").setMessage("需要 '对方RSA公钥'").setPositiveButton("好", null).create().show()
                return@setOnClickListener
            }
*/
            if (aesPlainText == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("需要 'AES加密/解密 明文'").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("需要明文")
                return@setOnClickListener
            }

            /** 生成32位长度的字符串用于AES */
            val aesPwd = secLib.genAesRandom()

            /** 密码用对方公钥加密 */
            // 取得对方公钥
            var aesKeyEncryptedViaOppPubKey = secLib.encryptByPublicKey(aesPwd, oppPubKey)
            // 错误处理
            if (aesKeyEncryptedViaOppPubKey == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("'对方RSA公钥' 加密AES密钥时发生错误").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("使用'对方RSA公钥'加密本次AES密钥时发生错误")
                return@setOnClickListener
            }

            /** 密码用己方公钥加密 */
            // 取得己方公钥
            var aesKeyEncryptedViaUrPubKey = secLib.encryptByPublicKey(aesPwd, urPubKey)
            // 错误处理
            if (aesKeyEncryptedViaUrPubKey == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("'己方RSA公钥' 加密AES密钥时发生错误").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("使用'己方RSA公钥'加密本次AES密钥时发生错误")
                return@setOnClickListener
            }

            /** AES加密明文框中的内容，aesPlainText */
            var aesEncryptedContent = secLib.aesEncrypt(aesPlainText, aesPwd.toString())
            // 错误处理
            if (aesEncryptedContent == null){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("AES加密明文时发生错误").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("使用本次AES密钥加密明文时发生错误")
                return@setOnClickListener
            }

            /** 己方私钥对AES密文签名 */
            var signature = secLib.sign(aesEncryptedContent, urPriKey)
            // 错误处理
            if(signature == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("'己方RSA私钥' 对AES密文签名时发生错误").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("使用'己方RSA私钥'对AES密文签名时发生错误")
                return@setOnClickListener
            }

            /** 用点号（.）拼接 */
            // 数据结构 => [密码用对方公钥加密].[密码用己方公钥加密].[签名].[密文内容]
            val finalContent = "$aesKeyEncryptedViaOppPubKey.$aesKeyEncryptedViaUrPubKey.$signature.$aesEncryptedContent"

            /** 将最终结果显示到AES密文框并复制到剪贴板 */
            ui.txtAesPlainText.text.clear() // 清空明文
            ui.txtAesCryptedText.text.clear() // 清空密文
            ui.txtAesCryptedText.setText(finalContent) // 放入新的内容

            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", finalContent)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
            } catch (e: Exception) {

            }

            ui.txtPrompt.setText("内容已加密并已经复制到剪贴板")
        }

        /** 解密密文并显示到明文框 */
        ui.btnAesDecrypt.setOnClickListener {
            var urMessage = false // 是否是己方发送给对方的消息

            // 己方私钥必须有
            //val urPriKey = ui.txtUrPriKey.text.toString()
            if (urPriKey == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("需要 '己方RSA私钥'").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("需要己方RSA私钥")
                return@setOnClickListener
            }

            /** 数据结构 => [密码用对方公钥加密].[密码用己方公钥加密].[签名].[密文内容] */
            val content = ui.txtAesCryptedText.text.toString()
            if (content == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("需要 'AES加密/解密 密文'").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("密文不能为空")
                return@setOnClickListener
            }

            val sector = content.split(".")
            if (sector.count() != 4){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("'AES加密/解密 密文' 数据格式错误").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("密文的数据格式错误")
                return@setOnClickListener
            }

            // 如果己方私钥能解开第一段RSA密文就可以得到AES密码，也就是对方发来的消息
            var aesKeyDecryptedViaUrPriKey = secLib.decryptByPrivateKey(sector[0], urPriKey)

            // 这里是己方私钥解不开第一段RSA密文，就解开第二段RSA密文得到AES密码，也就是己方向对方发送的消息
            if (aesKeyDecryptedViaUrPriKey == ""){
                aesKeyDecryptedViaUrPriKey = secLib.decryptByPrivateKey(sector[1], urPriKey)
                urMessage = true
            }
            // 如果再解不开，收工回家，报错得了
            if (aesKeyDecryptedViaUrPriKey == ""){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("无法使用 '己方RSA私钥' 取得AES密钥").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("无法使用己方RSA私钥取得此加密消息的密钥")
                return@setOnClickListener
            }

            // 这里肯定是已经获得了AES密码
            // AES解密数据 + 检查签名，签名有问题弹框框

            // 对方发送的，对方公钥验证签名
            if(!urMessage){
                // 对方公钥?
                //val oppPubKey = ui.txtOppRubKey.text.toString()
                // 无对方公钥则不验证
                if (oppPubKey == ""){
                    //AlertDialog.Builder(this).setTitle("提示").setMessage("缺少 '对方RSA公钥' 无法验证数据签名").setPositiveButton("好", null).create().show()
                    ui.txtPrompt.setText("缺少对方RSA公钥无法验证数据签名")
                }else{
                    if(!secLib.verify(sector[3], oppPubKey, sector[2])){
                        //AlertDialog.Builder(this).setTitle("提示").setMessage("验证签名失败，信息可能被篡改或者 '对方RSA公钥' 不匹配").setPositiveButton("好", null).create().show()
                        ui.txtPrompt.setText("验证签名失败，此加密消息可能被篡改或者由于对方RSA公钥不匹配")
                    }
                }
                // 己方发送的，不需要验证签名
            }else{

            }
            val decryptedContent = secLib.aesDecrypt(sector[3], aesKeyDecryptedViaUrPriKey!!)

            // 没东西就肯定解密失败了
            if (decryptedContent == null){
                //AlertDialog.Builder(this).setTitle("错误").setMessage("解密失败，可能是由于AES密钥不正确").setPositiveButton("好", null).create().show()
                ui.txtPrompt.setText("解密失败，可能是由于此加密消息的密钥不正确")
                return@setOnClickListener
            }

            // 解密完的数据出现在AES明文框里
            ui.txtAesPlainText.text.clear()
            ui.txtAesPlainText.setText(decryptedContent)
            ui.txtPrompt.setText("此加密消息已被解密")
        }

        /** 粘贴并解密 */
        ui.btnPasteAndDecrypt.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()
                    if(pasteString == ""){
                        return@setOnClickListener
                    }
                    ui.txtAesCryptedText.text.clear()
                    ui.txtAesCryptedText.setText(pasteString)
                    ui.btnAesDecrypt.callOnClick()
                }
            }catch (e: Exception){

            }
        }
    }
}