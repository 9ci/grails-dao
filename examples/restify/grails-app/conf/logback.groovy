import ch.qos.logback.classic.boolex.GEventEvaluator
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.filter.EvaluatorFilter
import grails.util.BuildSettings
import grails.util.Environment

import java.nio.charset.Charset

import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.core.spi.FilterReply.DENY
import static ch.qos.logback.core.spi.FilterReply.NEUTRAL

statusListener ch.qos.logback.core.status.NopStatusListener //turns off its own logging

def patternExpression = '%d{HH:mm:ss.SSS} [%t] %-5level %logger{48} - %msg%n'
// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    filter(EvaluatorFilter) {
        evaluator(GEventEvaluator) {
            //don't log errors and warnings
            expression = 'e.level.toInt() < WARN.toInt()' //
        }
        onMismatch = DENY
        onMatch = NEUTRAL
    }
    encoder(PatternLayoutEncoder) {
        charset = Charset.forName('UTF-8')
        pattern = patternExpression
    }
    //target = "System.out"
}

def rootAppenders = ["STDOUT"]

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode()  && targetDir != null) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    rootAppenders.add 'FULL_STACKTRACE'
}
if (Environment.getCurrent() == Environment.TEST && targetDir != null) {
    appender("TESTING-ERRORS", FileAppender) {
        filter(EvaluatorFilter) {
            evaluator(GEventEvaluator) {
                //only log ERROR and WARNINGS here no matter what gets set to root logger
                expression = 'e.level.toInt() >= WARN.toInt()'
            }
            onMatch = NEUTRAL
            onMismatch = DENY
        }
        file = "${targetDir}/TESTING-ERRORS.log"
        append = false
        encoder(PatternLayoutEncoder) {
            pattern = patternExpression
        }
    }
    logger("StackTrace", ERROR, ['TESTING-ERRORS'], false)
    rootAppenders.add 'TESTING-ERRORS'
}
root(DEBUG,rootAppenders)
