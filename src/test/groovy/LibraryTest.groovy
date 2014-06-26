/*
 * This Spock specification was auto generated by running 'gradle init --type groovy-library'
 * by 'MarkPerry' at '26/06/14 7:49 PM' with Gradle 1.12
 *
 * @author MarkPerry, @date 26/06/14 7:49 PM
 */

import spock.lang.Specification

class LibraryTest extends Specification{
    def "someLibraryMethod returns true"() {
        setup:
        Library lib = new Library()
        when:
        def result = lib.someLibraryMethod()
        then:
        result == true
    }
}