/*
 * © 2020. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.powerflow

import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class DummyTest extends Specification{
	def "Just test some shit positively"() {
		given:
		log.debug("Just test if 1 equals to 1.")

		expect:
		1 == 1
	}
}
