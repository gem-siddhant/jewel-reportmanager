package com.jewel.reportmanager.service;

import com.jewel.reportmanager.entity.DatabaseSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SequenceGenerator {
    @Autowired
    MongoOperations mongoOperations;

    public long generateSequence(String seqName) {

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(seqName));
        Update updation = new Update();
        updation.inc("seq", 1);

        DatabaseSequence counter = mongoOperations.findAndModify(query, updation,
                new FindAndModifyOptions().returnNew(true).upsert(true), DatabaseSequence.class);
        return !Objects.isNull(counter) ? counter.getSeq() : 1;
    }

}
