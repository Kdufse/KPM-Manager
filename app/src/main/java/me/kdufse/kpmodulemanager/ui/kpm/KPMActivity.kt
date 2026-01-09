package me.kdufse.kpmodulemanager.ui.kpm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kdufse.kpmodulemanager.R
import me.kdufse.kpmodulemanager.databinding.ActivityKpmBinding
import me.kdufse.kpmodulemanager.databinding.ItemKernelModuleBinding
import me.kdufse.kpmodulemanager.util.PreferenceManager
import me.kdufse.kpmodulemanager.util.RootStorageManager

class KPMActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKpmBinding
    private lateinit var rootStorageManager: RootStorageManager
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var moduleAdapter: KernelModuleAdapter
    private var superKey: String? = null
    private val kernelModules = mutableListOf<KernelModule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKpmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rootStorageManager = RootStorageManager(this)
        preferenceManager = PreferenceManager(this)

        setupUI()
        loadSuperKey()
        setupClickListeners()
    }

    private fun setupUI() {
        // 设置标题
        binding.toolbar.title = getString(R.string.module_list_title)
        
        // 设置返回按钮
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // 设置RecyclerView
        moduleAdapter = KernelModuleAdapter { module ->
            showUnloadModuleDialog(module)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@KPMActivity)
            adapter = moduleAdapter
            setHasFixedSize(true)
        }
        
        // 设置加载按钮
        binding.btnLoadModule.text = getString(R.string.module_list_load_module)
    }

    private fun loadSuperKey() {
        superKey = rootStorageManager.retrieveSuperKeyWithRoot()
        if (superKey.isNullOrEmpty()) {
            showEmptyView(getString(R.string.error_no_superkey))
        } else {
            loadKernelModules()
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        binding.btnLoadModule.setOnClickListener {
            // TODO: 实现加载模块功能
            Snackbar.make(binding.root, "加载模块功能", Snackbar.LENGTH_SHORT).show()
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            refreshModules()
        }
    }

    private fun refreshModules() {
        if (superKey.isNullOrEmpty()) {
            loadSuperKey()
        } else {
            loadKernelModules()
        }
    }

    private fun loadKernelModules() {
        binding.swipeRefresh.isRefreshing = true
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 执行 kpatch <superkey> kpm list
                val result = Shell.cmd("kpatch $superKey kpm list").exec()
                
                withContext(Dispatchers.Main) {
                    if (!result.isSuccess || result.out.isEmpty()) {
                        showEmptyView(getString(R.string.module_list_empty))
                        return@withContext
                    }
                    
                    val moduleNames = result.out.filter { it.isNotBlank() }
                    
                    if (moduleNames.isEmpty()) {
                        showEmptyView(getString(R.string.module_list_empty))
                        return@withContext
                    }
                    
                    kernelModules.clear()
                    
                    // 为每个模块获取详细信息
                    moduleNames.forEach { moduleName ->
                        fetchModuleInfo(moduleName)
                    }
                    
                    if (kernelModules.isEmpty()) {
                        showEmptyView(getString(R.string.module_list_empty))
                    } else {
                        showModuleList()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showEmptyView("加载失败: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun fetchModuleInfo(moduleName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 执行 kpatch <superkey> kpm info <moduleName>
                val result = Shell.cmd("kpatch $superKey kpm info $moduleName").exec()
                
                if (result.isSuccess) {
                    val info = parseModuleInfo(result.out, moduleName)
                    withContext(Dispatchers.Main) {
                        kernelModules.add(info)
                        moduleAdapter.submitList(kernelModules.toList())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseModuleInfo(output: List<String>, moduleName: String): KernelModule {
        var name = moduleName
        var version = "Unknown"
        var license = "Unknown"
        var author = "Unknown"
        var description = "No description"
        var args = "No arguments"
        
        output.forEach { line ->
            when {
                line.startsWith("name=") -> name = line.substringAfter("=")
                line.startsWith("version=") -> version = line.substringAfter("=")
                line.startsWith("license=") -> license = line.substringAfter("=")
                line.startsWith("author=") -> author = line.substringAfter("=")
                line.startsWith("description=") -> description = line.substringAfter("=")
                line.startsWith("args=") -> args = line.substringAfter("=")
            }
        }
        
        return KernelModule(name, version, license, author, description, args)
    }

    private fun showModuleList() {
        binding.emptyView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        moduleAdapter.submitList(kernelModules.toList())
    }

    private fun showEmptyView(message: String) {
        binding.tvEmptyMessage.text = message
        binding.emptyView.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        kernelModules.clear()
        moduleAdapter.submitList(emptyList())
    }

    private fun showUnloadModuleDialog(module: KernelModule) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("卸载模块")
            .setMessage("确定要卸载 ${module.name} 吗？")
            .setPositiveButton("确定") { dialog, _ ->
                unloadModule(module)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun unloadModule(module: KernelModule) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = Shell.cmd("kpatch $superKey kpm unload ${module.name}").exec()
                
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Snackbar.make(binding.root, "已卸载 ${module.name}", Snackbar.LENGTH_SHORT).show()
                        kernelModules.removeAll { it.name == module.name }
                        moduleAdapter.submitList(kernelModules.toList())
                        
                        if (kernelModules.isEmpty()) {
                            showEmptyView(getString(R.string.module_list_empty))
                        }
                    } else {
                        Snackbar.make(binding.root, "卸载失败: ${result.err}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "卸载失败: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (superKey != null) {
            refreshModules()
        }
    }
}

data class KernelModule(
    val name: String,
    val version: String,
    val license: String,
    val author: String,
    val description: String,
    val args: String
)

class KernelModuleAdapter(
    private val onUnloadClick: (KernelModule) -> Unit
) : RecyclerView.Adapter<KernelModuleAdapter.ViewHolder>() {

    private var modules: List<KernelModule> = emptyList()

    class ViewHolder(
        private val binding: ItemKernelModuleBinding,
        private val onUnloadClick: (KernelModule) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(module: KernelModule) {
            binding.tvModuleName.text = module.name
            binding.tvModuleVersion.text = "${module.version} | 作者：${module.author}"
            binding.tvModuleArgs.text = "参数：${module.args}"
            binding.tvModuleDescription.text = module.description
            
            binding.btnUnload.text = binding.root.context.getString(R.string.module_list_unload_module)
            binding.btnUnload.setOnClickListener {
                onUnloadClick(module)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemKernelModuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onUnloadClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(modules[position])
    }

    override fun getItemCount(): Int = modules.size

    fun submitList(newModules: List<KernelModule>) {
        modules = newModules
        notifyDataSetChanged()
    }
}