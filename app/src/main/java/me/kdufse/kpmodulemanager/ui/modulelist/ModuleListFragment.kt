package me.kdufse.kpmodulemanager.ui.modulelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import me.kdufse.kpmodulemanager.data.model.Module
import me.kdufse.kpmodulemanager.databinding.FragmentModuleListBinding
import me.kdufse.kpmodulemanager.ui.adapters.ModuleAdapter

class ModuleListFragment : Fragment() {

    private var _binding: FragmentModuleListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ModuleAdapter
    private var filterType: FilterType = FilterType.ALL

    enum class FilterType {
        ALL, ENABLED, DISABLED
    }

    companion object {
        private const val ARG_FILTER_TYPE = "filter_type"

        fun newInstance(filterType: FilterType): ModuleListFragment {
            val fragment = ModuleListFragment()
            val args = Bundle()
            args.putSerializable(ARG_FILTER_TYPE, filterType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filterType = it.getSerializable(ARG_FILTER_TYPE) as FilterType? ?: FilterType.ALL
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModuleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadModules()
        setupSwipeRefresh()
    }

    private fun setupRecyclerView() {
        adapter = ModuleAdapter { module ->
            // 点击跳转到详情页
            (activity as? MainActivity)?.let {
                // 这里可以跳转到详情页
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ModuleListFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun loadModules() {
        // 模拟数据
        val modules = listOf(
            Module(
                id = "1",
                name = "核心模块",
                packageName = "me.kdufse.kpmodulemanager.core",
                version = "1.0.0",
                versionCode = 1,
                description = "系统核心模块，提供基础功能",
                author = "KDufse",
                enabled = true,
                isSystem = true
            ),
            Module(
                id = "2",
                name = "主题引擎",
                packageName = "me.kdufse.themeengine",
                version = "2.1.0",
                versionCode = 21,
                description = "提供丰富的主题定制功能",
                author = "ThemeDev",
                enabled = true,
                isSystem = false
            ),
            Module(
                id = "3",
                name = "手势控制",
                packageName = "me.kdufse.gesturecontrol",
                version = "1.5.0",
                versionCode = 15,
                description = "强大的手势操作模块",
                author = "GestureTeam",
                enabled = false,
                isSystem = false
            )
        )

        val filteredModules = when (filterType) {
            FilterType.ALL -> modules
            FilterType.ENABLED -> modules.filter { it.enabled }
            FilterType.DISABLED -> modules.filter { !it.enabled }
        }

        adapter.submitList(filteredModules)
        binding.swipeRefresh.isRefreshing = false
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadModules()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}