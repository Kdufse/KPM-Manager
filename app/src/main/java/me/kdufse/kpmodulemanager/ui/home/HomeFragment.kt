class HomeFragment : Fragment() {

    private lateinit var rootStorageManager: RootStorageManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootStorageManager = RootStorageManager(requireContext())

        setupUI()
        loadSystemInfo()
        checkSuperUserStatus()
        checkRootStorageStatus()
        setupClickListeners()
    }

    private fun checkRootStorageStatus() {
        val hasRoot = rootStorageManager.hasRootAccess()
        val hasStoredKey = if (hasRoot) rootStorageManager.hasStoredSuperKey() else false

        binding.tvRootStorageStatus.text = if (hasRoot) {
            if (hasStoredKey) {
                "SuperKey已安全存储（Root位置）"
            } else {
                "Root可用，未存储SuperKey"
            }
        } else {
            "Root权限不可用"
        }

        binding.tvRootStorageStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                when {
                    hasRoot && hasStoredKey -> R.color.success_green
                    hasRoot -> R.color.warning_orange
                    else -> R.color.error_red
                }
            )
        )
    }

    private fun showSuperKeySetupDialog() {
        val dialogBinding = DialogSuperKeyBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.home_set_super_key)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.set) { dialog, _ ->
                val superKey = dialogBinding.etSuperKey.text.toString().trim()
                if (superKey.isNotEmpty()) {
                    // 使用Root权限存储到系统分区
                    val success = rootStorageManager.storeSuperKeyWithRoot(superKey)
                    if (success) {
                        showSuccessMessage("SuperKey已安全存储到系统分区")
                        checkRootStorageStatus()
                    } else {
                        showErrorMessage("存储失败，请检查Root权限")
                    }
                } else {
                    showErrorMessage("SuperKey不能为空")
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialogBinding.tvHint.text = "SuperKey将被加密后存储到/system/etc/superkey.prop（仅Root可访问）"
        dialog.show()
    }

    private fun handleSuperKeyOperations() {
    // 存储SuperKey
    val success = rootStorageManager.storeSuperKeyWithRoot("mySecretKey123")
    
    // 读取SuperKey
    val storedKey = rootStorageManager.retrieveSuperKeyWithRoot()
    
    // 检查是否存在
    val exists = rootStorageManager.hasStoredSuperKey()
    
    // 删除SuperKey
    val deleted = rootStorageManager.deleteStoredSuperKey()
    }

    private fun showSuperKeyInputDialog() {
        val dialogBinding = DialogSuperKeyBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.home_enter_super_key)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val inputKey = dialogBinding.etSuperKey.text.toString().trim()
                
                // 从Root存储位置读取SuperKey进行验证
                val storedKey = rootStorageManager.retrieveSuperKeyWithRoot()
                
                if (storedKey != null && inputKey == storedKey) {
                    showSuccessMessage("SuperKey验证成功")
                    navigateToModuleManager()
                } else {
                    showErrorMessage(getString(R.string.home_incorrect_super_key))
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialogBinding.tvHint.text = "请输入存储在系统分区中的SuperKey"
        dialog.show()
    }

    // 添加Root存储状态显示到UI
    private fun setupRootStorageUI() {
        binding.tvRootStorageStatus = TextView(requireContext()).apply {
            textSize = 12f
            setPadding(0, 8, 0, 0)
        }
        
        binding.capsuleContainer.addView(binding.tvRootStorageStatus)
    }
}