package chenxi.shangguan.messageciher_v202

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Contacts
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chenxi.shangguan.messageciher_v202.databinding.ActivityContactBookMainBinding
import chenxi.shangguan.messageciher_v202.databinding.ContactListBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ContactBookMain : AppCompatActivity() {

    private lateinit var ui : ActivityContactBookMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_contact_book_main)
        ui = ActivityContactBookMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // 去掉最顶上的刘海
        supportActionBar?.hide()

        /** 解密的AesMasterKey， 从验证Activity传过来的 */
        var masterAesKey = intent.getStringExtra("MasterAesKey")
        //AlertDialog.Builder(this).setTitle("开发测试").setMessage("MasterAesKey:\n$masterAesKey").setPositiveButton("好", null).create().show()

        // 密码库
        val secLib = SecurityLib()

        // 当前Activity加入到链表
        ExitApplication.instance!!.addActivity(this)

        val contactFile = File(filesDir, secLib.contactFileName)
        if (!contactFile.exists()){
            contactFile.createNewFile()
            if (!contactFile.exists()){
                AlertDialog.Builder(this).setTitle("错误").setMessage("无法创建通讯录").setPositiveButton("好", null).create().show()
            }else{
                AlertDialog.Builder(this).setTitle("提示").setMessage("成功创建通讯录").setPositiveButton("好", null).create().show()
            }
        }

        /**
         * 程序自毁，就是删掉2个文件然后后退出程序
         * */
        ui.btnDestory.setOnClickListener {
            AlertDialog.Builder(this).setTitle("敬告").setMessage("您将丢失所有数据，是否继续").setPositiveButton("是", DialogInterface.OnClickListener { dialogInterface, i ->
                contactFile.delete()
                val authFile = File(filesDir, secLib.authFileName)
                authFile.delete()
                ExitApplication.instance!!.exit()
            }).setNegativeButton("否", null).setOnDismissListener{}.create().show()
        }


        /**
         * 修改密码
         * */
        ui.btnChangeAuthCode.setOnClickListener {
            val i = Intent(this, ChangeAuthCode::class.java)
            startActivity(i)
        }

        /** 退出程序 */
        ui.btnExitProg.setOnClickListener {
            masterAesKey = ""
            ExitApplication.instance!!.exit()
        }

        /** 添加新的联系人 */
        ui.floatingBtnAddContact.setOnClickListener {
            // 不带任何参数跳转到 ”add or edit contact“ activity
            val i = Intent(this, ContactEdit::class.java)
            i.putExtra("MasterAesKey", masterAesKey)
            startActivity(i)
            loadContacts(masterAesKey)
        }

        /** 刷新视图的 */
        ui.btnReloadContactList.setOnClickListener{
            loadContacts(masterAesKey)
        }

        /**
         *  通讯录：recyclerView，数据类型的adapter，这个就比较复杂
         * */
        loadContacts(masterAesKey)
/*
        // 通讯录
        var items = mutableListOf<People>()
        // 读取文件，一行一行压进去
        val fos = FileInputStream(contactFile)
        var byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
        var len = fos.read(byte)
        var readContent = String(byte, 0, len)
        var sectors = readContent.split("\n")
        Log.e("开发测试 -> sectors.indices", "${sectors.indices.toString()}")
        for(i in sectors.indices){
            if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                val s = sectors[i].split(".")
                var p : People = People(s[0], s[1])
                //Log.e("通讯录第 ${i} 人 * s[0] * s[1]", "${i} * ${s[0]} * ${s[1]}")
                Log.e("开发测试 -> 通讯录第 ${i}", "人")
                //if(i != 0){
                    items.add(i, p)
                //}
            }
        }
        ui.contactList.adapter = PeopleAdapter(people = items, MasterAesKey = masterAesKey!!)
        ui.contactList.layoutManager = LinearLayoutManager(this)
        ui.contactList.adapter?.notifyDataSetChanged()

 */

    }

    inner class PeopleHolder(var ui: ContactListBinding) : RecyclerView.ViewHolder(ui.root) {}

    inner class PeopleAdapter(private val people: MutableList<People>, val MasterAesKey: String) : RecyclerView.Adapter<PeopleHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleHolder {
            val ui = ContactListBinding.inflate(layoutInflater, parent, false)
            return PeopleHolder(ui)
        }

        override fun onBindViewHolder(holder: PeopleHolder, position: Int) {
            val p = people[position]

            holder.ui.contactName.setText(p.name)

            holder.ui.edit.setOnClickListener {
                // 带参数跳到修改/增加联系人的Activity， 先用AES Master Key 解密第二段取出3段密钥
                val encryptedKeys = SecurityLib().aesDecrypt(p.encryptedKeys, MasterAesKey)!!.split(".")
                //Log.e("encryptedKeys ", encryptedKeys.size.toString())
                if (encryptedKeys.size != 3){
                    AlertDialog.Builder(holder.ui.root.context).setTitle("错误").setMessage("通讯录密钥不匹配，无法编辑").setPositiveButton("好", null).create().show()
                    return@setOnClickListener
                }

                // 传递到下一个Activity的参数
                val i = Intent(holder.ui.root.context, ContactEdit::class.java)
                i.putExtra("MasterAesKey", MasterAesKey)
                i.putExtra("Contact", p.name)
                i.putExtra("UrPubKey", encryptedKeys[0])
                i.putExtra("UrPriKey", encryptedKeys[1])
                i.putExtra("OppPubKey", encryptedKeys[2])
                startActivity(i)
                // Activity返回后，更新ListView
                loadContacts(MasterAesKey)
            }

            holder.ui.delete.setOnClickListener {
                AlertDialog.Builder(holder.ui.root.context).setTitle("敬告").setMessage("是否确定删除联系人：${p.name}").setPositiveButton("删除", DialogInterface.OnClickListener { dialogInterface, i ->
                    // 读入文本，找到那一行，删了，更新ListView
                    val contactFile = File(filesDir, SecurityLib().contactFileName)
                    val fosis = FileInputStream(contactFile)
                    val byte = ByteArray(fosis.available()) // 读入文件大小的byte之前要确定文件大小
                    val len = fosis.read(byte)
                    val readContent = String(byte, 0, len)
                    val sectors = readContent.split("\n").toMutableList()
                    var newStr = ""

                    //Log.e("开发测试", "联系人删除，旧的sectors.indices ${sectors.indices.toString()}")
                    //sectors.remove(p)

                    var index = 0
                    var isExist = false
                    for(i in sectors.indices){
                        if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                            var s = sectors[i].split(".")
                            /*
                            if (s[0] == p.name){
                                isExist = true
                                index = i
                                //sectors.removeAt(i)
                            }
                             */
                            if(s[0] != p.name){
                                newStr += sectors[i] + "\n"
                            }
                        }
                    }

                    /*
                    if(isExist){
                        sectors.removeAt(index)
                        Log.e("开发测试", "联系人删除 index:${index}，新的sectors.indices ${sectors.indices.toString()}")
                    }
                    var newStr = ""
                    for(i in sectors.indices){
                        if(sectors[i] != "") { // -1: 最后结尾的\n下一行不算进去
                            newStr += sectors[i] + "\n"
                        }
                    }

                     */
                    //contactFile.delete()
                    //contactFile.createNewFile()

                    //if (newStr != "") {
                        var fosOS = FileOutputStream(contactFile)
                        fosOS.write(newStr.toByteArray())
                        //fosOS.write("\n".toByteArray()) //写入换行
                        fosOS.close()
                    //}

                    // 更新View
                    loadContacts(MasterAesKey)
                    //people.remove(p)
                }).setOnDismissListener{}.setNegativeButton("取消", null).create().show()
            }

            // 转到加解密Activity
            holder.itemView.setOnClickListener {
                // 先用AES Master Key 解密第二段取出3段密钥，带参数跳转到CipherMain Activity
                val decryptedContent = SecurityLib().aesDecrypt(p.encryptedKeys, MasterAesKey)
                val encryptedKeys = decryptedContent!!.split(".")
/*
                Log.e("people.encryptedKeys", p.encryptedKeys)
                Log.e("decryptedContent", decryptedContent)
                Log.e("encryptedKeys RANGE", encryptedKeys.indices.toString())
                Log.e("RANGE Last", encryptedKeys.indices.last.toString())
                for(i in encryptedKeys.indices){
                    Log.e("${i} SECTOR", encryptedKeys[i])
                }
 */


                if (encryptedKeys.size != 3){
                    AlertDialog.Builder(holder.ui.root.context).setTitle("错误").setMessage("通讯录密钥不匹配").setPositiveButton("好", null).create().show()
                    return@setOnClickListener
                }

                val i = Intent(holder.ui.root.context, CipherMain::class.java)
                i.putExtra("Contact", p.name)
                i.putExtra("UrPubKey", encryptedKeys[0])
                i.putExtra("UrPriKey", encryptedKeys[1])
                i.putExtra("OppPubKey", encryptedKeys[2])
                startActivity(i)
            }
        }

        override fun getItemCount(): Int {
            return people.size
        }
    }

    override fun onResume() {
        super.onResume()
        //ui.contactList.adapter?.notifyDataSetChanged()
        //loadContacts(masterAesKey)
        ui.btnReloadContactList.callOnClick()
    }

    fun loadContacts(masterAesKey: String?){
        ui.contactList.removeAllViews()
        // 通讯录
        var items = mutableListOf<People>()
        // 读取文件，一行一行压进去
        var contactFile = File(filesDir, SecurityLib().contactFileName)
        var fos = FileInputStream(contactFile)
        var byte = ByteArray(fos.available()) // 读入文件大小的byte之前要确定文件大小
        var len = fos.read(byte)
        var readContent = String(byte, 0, len)
        //Log.e("开发测试 -> 文件内容", readContent)
        var sectors = readContent.split("\n")
        //Log.e("开发测试 -> sectors.indices", "${sectors.indices.toString()}")
        //Log.e("开发测试 -> sectors.size", "${sectors.size.toString()}")
        //for(i in sectors.indices){
        var i = 0
        while (i < sectors.size){
            //Log.e("开发测试 -> 通讯录第 ${i}", "人")
            if(sectors[i] != "" && sectors[i] != null) { // -1: 最后结尾的\n下一行不算进去
                val s = sectors[i].split(".")
                var p : People = People(s[0], s[1])
                //Log.e("通讯录第 ${i} 人 * s[0] * s[1]", "${i} * ${s[0]} * ${s[1]}")
                //Log.e("名字", s[0])
                //if(i != 0){
                items.add(i, p)
                //}
            }
            i++
        }

        ui.contactList.adapter = PeopleAdapter(people = items, MasterAesKey = masterAesKey!!)
        ui.contactList.layoutManager = LinearLayoutManager(this)
    }

}