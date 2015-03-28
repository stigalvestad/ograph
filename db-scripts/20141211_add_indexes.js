
// to drop an index:
//1. db.retrieval_log.getIndexes() pick name of index
//2. db.retrieval_log.dropIndex("timeStamp_1_eventorId_1_resourceType_1")

db.runners.ensureIndex({eventorId: 1}, {unique: true, dropDups: true})

db.race_results.ensureIndex({resultId: 1}, {unique: true, dropDups: true})

// if applied, this index creates issues for findAndModify with upsert => duplicate key. Cannot understand why
db.retrieval_log.ensureIndex({timeStamp: 1, eventorId: 1, resourceType: 1}, {unique: true, dropDups: true})
