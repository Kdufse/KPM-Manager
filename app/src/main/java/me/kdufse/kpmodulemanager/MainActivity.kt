package me.kdufse.kpmodulemanager

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import me.kdufse.kpmodulemanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupNavigation()
        setupBottomNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    private fun setupNavigation() {
        navController = findNavController(R.id.nav_host_fragment)
        
        // 配置AppBar，排除不需要显示返回按钮的顶级目的地
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_modules,
                R.id.navigation_settings,
                R.id.navigation_about
            )
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)
        
        // 监听导航变化，更新底部导航栏状态
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> {
                    binding.toolbar.title = getString(R.string.app_name)
                }
                R.id.navigation_modules -> {
                    binding.toolbar.title = getString(R.string.navigation_modules)
                }
                R.id.navigation_settings -> {
                    binding.toolbar.title = getString(R.string.navigation_settings)
                }
                R.id.navigation_about -> {
                    binding.toolbar.title = getString(R.string.navigation_about)
                }
            }
        }
        
        // 处理底部导航栏项目点击
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navigateToHome()
                    true
                }
                R.id.navigation_modules -> {
                    navigateToModules()
                    true
                }
                R.id.navigation_settings -> {
                    navigateToSettings()
                    true
                }
                R.id.navigation_about -> {
                    navigateToAbout()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToHome() {
        if (navController.currentDestination?.id != R.id.navigation_home) {
            navController.navigate(R.id.navigation_home)
        }
    }

    private fun navigateToModules() {
        if (navController.currentDestination?.id != R.id.navigation_modules) {
            navController.navigate(R.id.navigation_modules)
        }
    }

    private fun navigateToSettings() {
        if (navController.currentDestination?.id != R.id.navigation_settings) {
            navController.navigate(R.id.navigation_settings)
        }
    }

    private fun navigateToAbout() {
        if (navController.currentDestination?.id != R.id.navigation_about) {
            navController.navigate(R.id.navigation_about)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_search -> {
                // 处理搜索操作
                true
            }
            R.id.action_refresh -> {
                // 处理刷新操作
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id in setOf(
                R.id.navigation_home,
                R.id.navigation_modules,
                R.id.navigation_settings,
                R.id.navigation_about
            )
        ) {
            // 如果在顶级目的地，正常退出应用
            finish()
        } else {
            // 否则执行正常的返回操作
            super.onBackPressed()
        }
    }
}