# Debit card app

Simple application for charging debit card.

## Assumptions

* Debit card has some debit limit. 
* Card cannot be charged if balance after that will be below limit.
* Card can be pay off - it simple increase a balance.
* Card also can be blocked by user or system
  * once this is done it cannot be charged anymore
  * blocked card still can be pay off
  * Card can be unblocked by system only

## Simplifications
Since this is only sample application, authentication module is skipped for now.

## How to run application
Prerequisite
Java 20 or higher
Gradle 8.3
Docker installed

### Local mode
1. Run `gradle localRun`

### Docker mode
1. Run `gradle build`
2. Run `docker-compose up`
