package main.kotlin

class Sample(private val name: String) {
    companion object {
        private var LATEST_ID = 1
    }

    fun generateUser(): User {
        return User.with {
            id = LATEST_ID++
            name = this@Sample.name
            programingLanguages = listOf(
                    ProgramingLanguage.with {
                        name = "Kotlin"
                        yearsLearning(5)
                    },
                    ProgramingLanguage.with {
                        name = "Java"
                        yearsLearning(10)
                    }
            )
        }
    }

}


fun main() {
    val sample = Sample("Panini")

    val user = sample.generateUser()

    println(user)
    println(user.name)
    user.programingLanguages.forEach {
        println("learned ${it.name} for ${it.yearsLearning} years")
    }
}