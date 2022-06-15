package com.grs.core.repo.grs;

import com.grs.core.domain.grs.Complainant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Created by Acer on 9/27/2017.
 */
@Repository
public interface ComplainantRepo extends JpaRepository<Complainant, Long> {
    public Complainant findByUsername(String Username);
    public Complainant findByUsernameAndPassword(String Username, String password);
    public Complainant findByPhoneNumber(String phoneNumber);
    public List<Complainant> findByPhoneNumberIsContaining(String phoneNumber);
}
