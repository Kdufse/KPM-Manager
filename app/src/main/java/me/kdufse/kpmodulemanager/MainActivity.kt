package me.kdufse.kpmodulemanager

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import me.kdufse.kpmodulemanager.databinding.ActivityMainBinding
import me.kdufse.kpmodulemanager.ui.modulelist.ModuleListFragment
import me.kdufse.kpmodulemanager.ui.settings.SettingsFragment
import me.kdufse.kpmodulemanager.ui.updates.UpdatesFragment

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private val fragmentTitles = arrayOf("全部模块", "已启用", "已禁用", "更新")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
        setupNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragmentTitles.size

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ModuleListFragment.newInstance(ModuleListFragment.FilterType.ALL)
                    1 -> ModuleListFragment.newInstance(ModuleListFragment.FilterType.ENABLED)
                    2 -> ModuleListFragment.newInstance(ModuleListFragment.FilterType.DISABLED)
                    3 -> UpdatesFragment.newInstance()
                    else -> ModuleListFragment.newInstance(ModuleListFragment.FilterType.ALL)
                }
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = fragmentTitles[position]
        }.attach()

        binding.bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_updates -> {
                    binding.viewPager.currentItem = 3
                    true
                }
                R.id.nav_settings -> {
                    // 可以切换到设置页面
                    Toast.makeText(this, "设置", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                Toast.makeText(this, "搜索", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_refresh -> {
                Toast.makeText(this, "刷新", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_about -> {
                // 显示关于对话框
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_installed -> {
                binding.viewPager.currentItem = 0
            }
            R.id.nav_repository -> {
                Toast.makeText(this, "模块仓库", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_backup -> {
                Toast.makeText(this, "备份与恢复", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logs -> {
                Toast.makeText(this, "日志", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}