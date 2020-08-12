package yakworks.taskify.domain

import java.time.LocalDate
import java.time.LocalDateTime


class Contact {
    static hasOne = [address: ContactAddress]
    Salutations salutation
    String firstName
    String lastName
    String email

    LocalDate dateOfBirth
    TimeZone timeZone
    LocalDateTime activateOnDate

    Integer age

    Date dateCreated
    Date lastUpdated
    Boolean inactive

    static constraints = {
        firstName nullable: false
        dateOfBirth nullable: true
        inactive bindable: false
    }

    enum Salutations {
        Ninja,
        Mr,
        Mrs,
        Ms,
        Dr,
        Rev
    }
}
