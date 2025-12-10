package com.example.distributedtaskboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onTaskCompleted: (Task) -> Unit,
    private val onTaskUpdated: (Task) -> Unit,
    private val onTaskDeleted: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.checkBox.text = currentTask.description
        
        // Show who last modified this task
        holder.modifiedByText.text = "Modified by: ${currentTask.lastModifiedBy}"

        // Temporarily remove the listener to prevent the infinite loop
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = currentTask.isCompleted

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            currentTask.isCompleted = isChecked
            onTaskCompleted(currentTask)
        }

        holder.updateButton.setOnClickListener {
            onTaskUpdated(currentTask)
        }

        holder.deleteButton.setOnClickListener {
            onTaskDeleted(currentTask)
        }
    }

    override fun getItemCount() = tasks.size

    fun addTask(task: Task) {
        tasks.add(task)
        notifyItemInserted(tasks.size - 1)
    }

    fun removeTask(task: Task) {
        val position = tasks.indexOf(task)
        if (position > -1) {
            tasks.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun removeTaskById(id: Int) {
        val position = tasks.indexOfFirst { it.id == id }
        if (position > -1) {
            tasks.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateTask(task: Task) {
        val position = tasks.indexOfFirst { it.id == task.id }
        if (position > -1) {
            tasks[position] = task
            notifyItemChanged(position)
        }
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxTask)
        val modifiedByText: TextView = itemView.findViewById(R.id.textModifiedBy)
        val updateButton: ImageButton = itemView.findViewById(R.id.buttonUpdate)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }
}
