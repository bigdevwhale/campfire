package app.campfire.domain

/**
 * How the fire looks, and how deep its questions go. The fire burns down through the evening:
 * flares first, settles into logs, and what is left asks the quietest things.
 */
enum class Stage {
    FLAME, LOGS, EMBERS;

    companion object {
        /** The stage the fire is in by how many logs still hold heat. */
        fun of(logs: Int): Stage = when {
            logs >= 5 -> FLAME
            logs >= 2 -> LOGS
            else -> EMBERS
        }
    }
}

/** One spark. Ids are stable (and never 0, which means "no ember"), so the bag can remember what has been asked. */
data class Question(val id: Int, val stage: Stage, val ru: String, val en: String) {
    fun text(ru: Boolean) = if (ru) this.ru else en
}

private fun bank(stage: Stage, ru: List<String>, en: List<String>) =
    ru.indices.map { Question(stage.ordinal * 100 + it + 1, stage, ru[it], en[it]) }

/** The whole deck: playful at full flame, closer over burning logs, sleepy in the embers. */
val QUESTIONS: List<Question> =
    bank(
        Stage.FLAME,
        listOf(
            "Куда бы мы отправились прямо сейчас, если бы всё можно?",
            "Если бы у тебя был хвост — какой?",
            "Какое твоё умение, о котором почти никто не знает?",
            "Что бы ты взял с собой в поход из нашего дома?",
            "Если бы у нас жил дракон — как бы его звали и чем бы он питался?",
            "Какой звук ты умеешь издавать лучше всех?",
            "Придумай блюдо и дай ему имя.",
            "Какое слово тебе нравится произносить?",
            "Кем бы ты был в цирке?",
            "Самый смешной сон, который ты помнишь?",
            "Если бы день был на два часа длиннее — на что бы ты их потратил?",
            "Какой подарок можно придумать без денег?",
            "Если бы можно было переименовать нашу улицу — как?",
            "Что ты умеешь делать руками?",
            "Какое животное ты бы взял в напарники?",
            "Что бы ты положил в машину времени?",
        ),
        listOf(
            "Where would we go right now, if anything were possible?",
            "If you had a tail — what would it be like?",
            "What can you do that almost nobody knows about?",
            "What would you take on a hike from our home?",
            "If a dragon lived with us — its name, and what would it eat?",
            "What sound can you make better than anyone?",
            "Invent a dish and give it a name.",
            "What word do you love saying out loud?",
            "What would you be in a circus?",
            "The funniest dream you remember?",
            "If the day were two hours longer, what would you spend them on?",
            "What gift can you invent without any money?",
            "If you could rename our street — to what?",
            "What can your hands do?",
            "Which animal would you take as a sidekick?",
            "What would you put into a time machine?",
        ),
    ) +
        bank(
            Stage.LOGS,
            listOf(
                "Что тебе сегодня удалось?",
                "За что ты сегодня можешь сказать себе спасибо?",
                "Что ты сегодня не сказал вслух?",
                "Кто тебе помог на этой неделе — и чем?",
                "Что тебя сейчас делает сильнее?",
                "О чём ты мечтаешь втихомолку?",
                "Чему ты хочешь научиться в этом году?",
                "Какая мелочь тебя недавно порадовала?",
                "Что ты откладываешь на потом?",
                "Куда бы ты хотел сходить вдвоём?",
                "Что для тебя «по-настоящему дома»?",
                "Чем ты гордишься из того, что никто не заметил?",
                "Что тебя пугает, но чуть меньше, чем раньше?",
                "Какой совет ты бы дал себе год назад?",
                "Что тебе сейчас нужно от нас?",
                "О чём тебе легко со мной говорить?",
            ),
            listOf(
                "What went well for you today?",
                "What can you thank yourself for today?",
                "What did you leave unsaid today?",
                "Who helped you this week — and how?",
                "What is making you stronger right now?",
                "What do you quietly dream about?",
                "What do you want to learn this year?",
                "What small thing made you happy recently?",
                "What are you putting off?",
                "Where would you like to go together?",
                "What feels like really home to you?",
                "What are you proud of that nobody noticed?",
                "What scares you a bit less than it used to?",
                "What advice would you give yourself a year ago?",
                "What do you need from us right now?",
                "What is easy to talk about with me?",
            ),
        ) +
        bank(
            Stage.EMBERS,
            listOf(
                "Что тебе снилось?",
                "Что было тёплого сегодня?",
                "Что ты хочешь через год?",
                "Кого тебе приятно было увидеть сегодня?",
                "Что ты оставишь сегодняшнему дню?",
                "Если бы сегодня был фильм — как бы он назывался?",
                "Что ты скажешь сегодняшнему дню, засыпая?",
                "Какое у тебя сейчас самое тихое желание?",
                "Что доброе тебе вспомнилось?",
                "Где ты был сегодня счастлив — хоть на минуту?",
                "Что из этого дня ты поставил бы на видное место?",
                "Каким ты хочешь увидеть завтрашний день?",
                "Что тебе сказать — и можно спать?",
                "Что сегодня было на вкус?",
                "Какое слово сегодня твоё?",
                "Что ты заберёшь с собой из этого вечера?",
            ),
            listOf(
                "What did you dream about?",
                "What was warm about today?",
                "What do you want a year from now?",
                "Who were you glad to see today?",
                "What will you leave to this day?",
                "If today were a film — what would it be called?",
                "What will you tell today as you fall asleep?",
                "What is your quietest wish right now?",
                "What kind thing came back to you?",
                "Where were you happy today, even for a minute?",
                "What from this day would you put on display?",
                "How do you want tomorrow to look?",
                "What should I say to you — so you can sleep?",
                "What did today taste like?",
                "Which word was yours today?",
                "What will you take from this evening?",
            ),
        )

/**
 * Draws questions without repeating one, each stage on its own: when a stage's whole pool has
 * been asked, that pool quietly reshuffles. Pure, so it can be tested on the desk.
 */
class QuestionBag(private val used: MutableSet<Int> = mutableSetOf()) {

    fun draw(stage: Stage): Question {
        val pool = QUESTIONS.filter { it.stage == stage }
        val fresh = pool.filter { it.id !in used }
        val pick = if (fresh.isEmpty()) {
            // The whole stage has been asked: it starts over.
            pool.forEach { used.remove(it.id) }
            pool.random()
        } else {
            fresh.random()
        }
        used.add(pick.id)
        return pick
    }

    /** For the ember: anything not asked recently, whatever stage it sleeps in. */
    fun drawEmber(): Question {
        val fresh = QUESTIONS.filter { it.id !in used }
        val pick = (fresh.ifEmpty { QUESTIONS }).random()
        used.add(pick.id)
        return pick
    }

    fun usedIds(): Set<Int> = used.toSet()

    fun restore(ids: Set<Int>) {
        used.clear()
        used.addAll(ids)
    }
}
