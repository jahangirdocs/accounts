# accounts
This is accounts application service that provides feature for
1) creating new account
2) get account by id
3) transfer amount from one account to another account.

# Following thing can be done to make the current application production ready
1) In memory repository replaced with persistence repository.
2) Transaction management can be done using spring transaction.
3) Fo production ready system transaction management using JTA and row level optimistic locking can be done using persistent store.
4) Sequencing of transaction in persistence store like DB using FIFO Queue in DB table group by participant Accounts taking part in transaction. this means there will be separate FIFO Queue for same Accont participating in several transaction.


# Add on things that can be added
1) Scenario testing can be done via non-invasive Testing framework like Cucumber.
2) Application can be containerized using docker.
