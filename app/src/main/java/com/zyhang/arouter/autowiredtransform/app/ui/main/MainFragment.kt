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
import com.zyhang.arouter.autowiredtransform.app.JavaActivity
import com.zyhang.arouter.autowiredtransform.app.databinding.MainFragmentBinding

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

    private lateinit var binding: MainFragmentBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MainFragmentBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel

        binding.message.text = "name: $name" +
                "\n" +
                "age: $age"

        binding.message.setOnClickListener {
            startActivity(Intent(context, JavaActivity::class.java).apply {
                putExtra("i", 101)
            })
        }
    }
}
