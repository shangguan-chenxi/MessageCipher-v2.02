package chenxi.shangguan.messageciher_v202

import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import chenxi.shangguan.messageciher_v202.databinding.ActivityContactEditBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ContactEdit : AppCompatActivity() {

    private lateinit var ui : ActivityContactEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_contact_edit)
        ui = ActivityContactEditBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        // 密码库
        val secLib = SecurityLib()

        // 清空剪贴板
        /*
        try{
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val mClipData = ClipData.newPlainText("", "")
            cm.setPrimaryClip(mClipData)
        }catch (e: Exception){

        }

         */

        val contactFile = File(filesDir, secLib.contactFileName)
        val fos = FileInputStream(contactFile)
        val byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
        val len = fos.read(byte)
        val readContent = String(byte, 0, len)
        var sectors = readContent.split("\n").toMutableList()


        // 上一个Activity传递过来的参数
        val masterAesKey = intent.getStringExtra("MasterAesKey")
        val contactName = intent.getStringExtra("Contact")
        val urPubKey = intent.getStringExtra("UrPubKey")
        val urPriKey = intent.getStringExtra("UrPriKey")
        val oppPubKey = intent.getStringExtra("OppPubKey")

        ui.txtContactName.setText(contactName)
        ui.txtUrPubKey.setText(urPubKey)
        ui.txtUrPriKey.setText(urPriKey)
        ui.txtOppRubKey.setText(oppPubKey)


        /** 清空密钥 */
        ui.btnPurge.setOnClickListener {
            ui.txtUrPubKey.text.clear()
            ui.txtUrPriKey.text.clear()
            ui.txtOppRubKey.text.clear()
            ui.txtHint.setText("已清空所有密钥")
        }

        /** 生成RSA密钥对 */
        ui.btnGenRSAKey.setOnClickListener {
            var keys: Map<String, Any>? = secLib.initKey()
            ui.txtUrPubKey.setText(secLib.getPublicKey(keys!!)) // 公钥
            ui.txtUrPriKey.setText(secLib.getPrivateKey(keys!!)) // 私钥
            ui.txtHint.setText("成功生成RSA密钥对")
        }

        /** 复制己方RSA公钥到剪贴板 */
        ui.btnCopyUrPubKey.setOnClickListener {
            val text = ui.txtUrPubKey.text.toString()
            if(text == ""){
                ui.txtHint.setText("己方公钥不能为空")
                return@setOnClickListener
            }

            // 将内容复制到剪贴板
            try {
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                // 创建普通字符型ClipData
                val mClipData = ClipData.newPlainText("Label", text)
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);

            } catch (e: Exception) {

            }
            ui.txtHint.setText("已复制己方公钥到剪贴板")
        }

        /** 从剪贴板中取得对方RSA公钥并显示到对方公钥框 */
        ui.btnPasteOppPubKey.setOnClickListener {
            // 从剪贴板获取内容
            try{
                //获取剪贴板管理器
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                //获取文本
                val clipData: ClipData? = cm.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text
                    val pasteString = text.toString()

                    ui.txtOppRubKey.text.clear()
                    ui.txtOppRubKey.setText(pasteString)
                    ui.txtHint.setText("已复制公钥到对方RSA公钥文本框")
                }
            }catch (e: Exception){

            }
        }

        /**  */
        ui.btnSaveContact.setOnClickListener {

            val newContactName = ui.txtContactName.text.toString()
            val newUrPubKey = ui.txtUrPubKey.text.toString()
            val newUrPriKey = ui.txtUrPriKey.text.toString()
            val newOppPubKey = ui.txtOppRubKey.text.toString()

            if(newContactName == ""){
                ui.txtHint.setText("联系人名称不能为空")
                return@setOnClickListener
            }

            if(newUrPubKey == ""){
                ui.txtHint.setText("己方RSA公钥不能为空")
                return@setOnClickListener
            }

            if(newUrPriKey == ""){
                ui.txtHint.setText("己方RSA私钥不能为空")
                return@setOnClickListener
            }

            if(newOppPubKey == ""){
                ui.txtHint.setText("对方RSA公钥不能为空")
                return@setOnClickListener
            }

            val data = "${newUrPubKey}.${newUrPriKey}.${newOppPubKey}"
            val keys = secLib.aesEncrypt(data, masterAesKey!!)
            val str = "${newContactName}.${keys}"

            var index = 0
            var isExist = false
            //Log.e("开发测试", "新建编辑联系人sectors.indices：${sectors.indices.toString()}")
            for(i in sectors.indices){
                if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                    var s = sectors[i].split(".")
                    if (s[0] == contactName || s[0] == newContactName){
                        //Log.e("开发测试", "输入的联系人${newContactName} ：传入的联系人${contactName}：当前联系人s[0] ${s[0]}")
                        isExist = true
                        index = i
                        break
                    }
                }
            }

            // 编辑现存
            if (isExist && contactName != null){
                //Log.e("开发测试", "输入的联系人${contactName} ：存在，编辑")
                sectors[index] = str // 修改

                var newStr = ""
                for(i in sectors.indices){
                    if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                        newStr += sectors[i] + "\n"
                    }
                }
                val fos = FileOutputStream(contactFile)
                fos.write(newStr.toByteArray())
                //fos.write("\n".toByteArray()) //写入换行
                fos.close()

                AlertDialog.Builder(this).setTitle("提示").setMessage("联系人编辑成功").setPositiveButton("好", DialogInterface.OnClickListener { dialogInterface, i ->
                    finish()
                }).setOnDismissListener{
                    finish()
                }.create().show()
            }else if(isExist && contactName == null) {
                AlertDialog.Builder(this).setTitle("提示").setMessage("重复的联系人，请检查").setPositiveButton("好", null).setOnDismissListener{}.create().show()
            }else{
                // 添加新人
                //Log.e("开发测试", "输入的联系人${newContactName} ：不存在，添加新人")
                contactFile.appendBytes(str.toByteArray())
                contactFile.appendBytes("\n".toByteArray())

                /*
                val testStr = str.split(".")
                val testSector = secLib.aesDecrypt(testStr[1], masterAesKey)
                val testKeys = testSector!!.split(".")
                for (i in testKeys.indices){
                    Log.e("${i} SECTOR", testKeys[i])
                }
                */

                AlertDialog.Builder(this).setTitle("提示").setMessage("联系人添加成功").setPositiveButton("好", DialogInterface.OnClickListener { dialogInterface, i ->
                    finish()
                }).setOnDismissListener{
                    finish()
                }.create().show()
            }
        }
    }
}