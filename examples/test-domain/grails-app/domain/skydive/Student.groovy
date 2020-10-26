package skydive

class Student {
    Jumper jumper
    String name
    String studentId

    static constraints = {
        studentId unique:true
    }
}
