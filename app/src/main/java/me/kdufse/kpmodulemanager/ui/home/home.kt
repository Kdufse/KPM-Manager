package me.kdufse.kpmodulemanager.ui.home

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import me.kdufse.kpmodulemanager.R
import me.kdufse.kpmodulemanager.databinding.FragmentHomeBinding
import me.kdufse.kpmodulemanager.databinding.DialogSuperKeyBinding
import me.kdufse.kpmodulemanager.util.PreferenceManager
import me.kdufse.kpmodulemanager.util.SystemInfo
import me.kdufse.kpmodulemanager.util.SuperUserManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceManager: PreferenceManager
    private lateinit var superUserManager: SuperUserManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 处理权限请求结果
        checkSuperUserStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceManager = PreferenceManager(requireContext())
        superUserManager = SuperUserManager(requireContext())

        setupUI()
        loadSystemInfo()
        checkSuperUserStatus()
        setupClickListeners()
    }

    private fun setupUI() {
        // 设置圆形胶囊框的样式
        binding.capsuleContainer.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.bg_capsule
        )

        // 设置圆角矩形框的样式
        binding.roundedRectContainer.background = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.bg_rounded_rect
        )
    }

    private fun loadSystemInfo() {
        // 更新圆形胶囊框中的信息
        binding.tvKernelVersion.text = SystemInfo.getKernelVersion()
        binding.tvKernelArch.text = SystemInfo.getKernelArchitecture()
        binding.tvLoadedModules.text = getString(
            R.string.home_loaded_modules,
            SystemInfo.getLoadedModulesCount()
        )

        // 更新圆角矩形框中的信息
        binding.tvDeviceModel.text = SystemInfo.getDeviceModel()
        binding.tvAndroidVersion.text = SystemInfo.getAndroidVersion()
        binding.tvManagerVersion.text = SystemInfo.getManagerVersion(requireContext())
    }

    private fun checkSuperUserStatus() {
        val hasSuperUser = superUserManager.checkSuperUserPermission()
        binding.tvSuperUserStatus.text = if (hasSuperUser) {
            getString(R.string.home_superuser_granted)
        } else {
            getString(R.string.home_superuser_denied)
        }

        binding.tvSuperUserStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (hasSuperUser) R.color.success_green else R.color.error_red
            )
        )

        // 检查超级密钥状态
        checkSuperKeyStatus()
    }

    private fun checkSuperKeyStatus() {
        val hasSuperKey = preferenceManager.hasSuperKey()
        binding.tvSuperKeyStatus.text = if (hasSuperKey) {
            getString(R.string.home_super_key)
        } else {
            getString(R.string.home_no_super_key)
        }

        binding.tvSuperKeyStatus.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (hasSuperKey) R.color.success_green else R.color.warning_orange
            )
        )

        binding.ivSuperKey.setImageResource(
            if (hasSuperKey) R.drawable.ic_key_set else R.drawable.ic_key_not_set
        )
    }

    private fun setupClickListeners() {
        // 超级密钥区域点击事件
        binding.superKeyArea.setOnClickListener {
            if (preferenceManager.hasSuperKey()) {
                showSuperKeyInputDialog()
            } else {
                showSuperKeySetupDialog()
            }
        }

        // 超级用户状态区域点击事件
        binding.superUserArea.setOnClickListener {
            if (!superUserManager.checkSuperUserPermission()) {
                requestSuperUserPermission()
            }
        }

        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            refreshSystemInfo()
        }
    }

    private fun showSuperKeySetupDialog() {
        val dialogBinding = DialogSuperKeyBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.home_set_super_key)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.set) { dialog, _ ->
                val superKey = dialogBinding.etSuperKey.text.toString().trim()
                if (superKey.isNotEmpty()) {
                    preferenceManager.setSuperKey(superKey)
                    checkSuperKeyStatus()
                    showSuccessMessage("超级密钥设置成功")
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialogBinding.tvHint.text = getString(R.string.home_super_key_hint)
        dialog.show()
    }

    private fun showSuperKeyInputDialog() {
        val dialogBinding = DialogSuperKeyBinding.inflate(layoutInflater)
        
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.home_enter_super_key)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val inputKey = dialogBinding.etSuperKey.text.toString().trim()
                val storedKey = preferenceManager.getSuperKey()
                
                if (inputKey == storedKey) {
                    showSuccessMessage("超级密钥验证成功")
                    // 这里可以跳转到管理界面
                } else {
                    showErrorMessage(getString(R.string.home_incorrect_super_key))
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialogBinding.tvHint.text = getString(R.string.home_super_key_hint)
        dialog.show()
    }

    private fun requestSuperUserPermission() {
        superUserManager.requestSuperUserPermission { granted ->
            if (granted) {
                showSuccessMessage("超级用户权限获取成功")
                checkSuperUserStatus()
            } else {
                showErrorMessage("超级用户权限获取失败")
            }
        }
    }

    private fun refreshSystemInfo() {
        binding.progressBar.visibility = View.VISIBLE
        
        // 模拟刷新过程
        binding.root.postDelayed({
            loadSystemInfo()
            checkSuperUserStatus()
            binding.progressBar.visibility = View.GONE
            showSuccessMessage("系统信息已刷新")
        }, 1000)
    }

    private fun showSuccessMessage(message: String) {
        // 可以使用Snackbar或Toast显示成功消息
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }
}