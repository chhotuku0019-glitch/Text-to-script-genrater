package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE status = 'Draft' ORDER BY updatedAt DESC LIMIT 5")
    fun getRecentDrafts(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY updatedAt DESC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectFlowById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE title LIKE '%' || :query || '%' OR currentScript LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // Script Versions
    @Query("SELECT * FROM script_versions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getVersionsForProject(projectId: Long): Flow<List<ScriptVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: ScriptVersionEntity): Long

    @Query("DELETE FROM script_versions WHERE id = :versionId")
    suspend fun deleteVersionById(versionId: Long)

    @Query("DELETE FROM script_versions WHERE projectId = :projectId")
    suspend fun deleteVersionsForProject(projectId: Long)
}
