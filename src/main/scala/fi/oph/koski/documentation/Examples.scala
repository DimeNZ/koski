package fi.oph.koski.documentation

import fi.oph.koski.tiedonsiirto.ExamplesTiedonsiirto

object Examples {
  val examples = ExamplesAmmatillinen.examples ++ ExamplesValma.examples ++ ExamplesTelma.examples ++
    ExamplesPerusopetukseenValmistavaOpetus.examples ++ ExamplesPerusopetus.examples ++ ExamplesPerusopetuksenLisaopetus.examples ++ ExamplesLukioonValmistavaKoulutus.examples ++
    ExamplesLukio.examples ++ ExamplesYlioppilastutkinto.examples ++
    ExamplesKorkeakoulu.examples ++ ExamplesTiedonsiirto.examples ++ ExamplesIB.examples

  val allExamples = examples
}
