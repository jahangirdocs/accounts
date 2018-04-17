# Accounts Controller

POST
/v1/accounts
createAccount

POST
/v1/accounts/transfer
transferBalance

GET
/v1/accounts/{accountId}
getAccount

# Following thing can be done to make the current application production ready
1) In memory repository replaced with persistence repository.
2) Transaction management can be done using spring transaction.
3) Fo production ready system transaction management using JTA and row level optimistic locking can be done using persistent store.
4) Sequencing of transaction in persistence store like DB using FIFO Queue in DB table group by participant Accounts taking part in transaction. this means there will be separate FIFO Queue for same Accont participating in several transaction.

# API Documentation
swagger is added to generate API documentation. api doc is available at /v2/api-docs
# Production ready feature enable 
spring-boot-starter-actuator dependency is added to enable production ready features like
health  
auditevents
metrics
mappings


# Future scope
1) Scenario testing can be done via non-invasive Testing framework like Cucumber.
2) Application can be containerized using docker.
