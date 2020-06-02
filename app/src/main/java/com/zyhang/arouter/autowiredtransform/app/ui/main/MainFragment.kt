package com.zyhang.arouter.autowiredtransform.app.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.zyhang.arouter.autowiredtransform.app.JavaActivity
import com.zyhang.arouter.autowiredtransform.app.R
import com.zyhang.arouter.autowiredtransform.app.`JavaActivity$$ARouter$$Autowired`
import kotlinx.android.synthetic.main.main_fragment.*
import java.util.*

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment().apply {
            arguments = Bundle().apply {
                putString("name", "John")
                putInt("age", 18)
            }
        }
    }

    @JvmField
    @Autowired
    var name = "null"

    @JvmField
    @Autowired
    var age = 0

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel

        message.text = "name: $name" +
                "\n" +
                "age: $age"

        message.setOnClickListener {
            startActivity(Intent(context, JavaActivity::class.java).apply {
                putExtra("i", 101)
            })
        }
    }
}
