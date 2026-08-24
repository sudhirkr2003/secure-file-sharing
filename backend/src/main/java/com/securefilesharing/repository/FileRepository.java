package com.securefilesharing.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.securefilesharing.entity.File;

public interface FileRepository extends JpaRepository<File, Long>
{
}
