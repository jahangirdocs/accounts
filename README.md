# accounts
This is accounts application service that provides feature for
1) creating new account
2) get account by id
3) transfer amount from one account to another account.

# Following thing can be done to make the current application production ready
1) In memory repository replaced with persistence repository.
2) Transaction management can be done using spring transaction.
3) To avoid concurrent read write of same account by multipel thread we can implement Sequencing of transaction in persistence store like DB. lock should be aquired on the keys accountId that are participating in transaction then perform transaction and release lock.
To achive this we can design multiple FIFO Queues group by participant
Here if from account 'A' is amount is transferred to account 'B' then 'AB' and 'BA' both should be locked in same FIFO Queue
and then it should try to aquire lock for 'N' unit of time if lock is successfully aquired then perform transaction otherwise fail.
Here persistence store lebel record locking will be helpful in production as there might be multiple nodes running in production cluster

#Add on things that can be added
1) Scenario testing can be done via non invasive Testing framework like Cucumber.
2) Application can be containerized using docker.
