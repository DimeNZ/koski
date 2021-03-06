describe('Lukiokoulutus', function( ){
  var page = KoskiPage()
  var todistus = TodistusPage()
  var opinnot = OpinnotPage()
  before(Authentication().login(), resetFixtures)

  describe('Lukion päättötodistus', function() {
    before(page.openPage, page.oppijaHaku.search('110496-9369', page.isOppijaSelected('Liisa')))
    describe('Oppijan suorituksissa', function() {
      it('näytetään', function() {
        expect(opinnot.getTutkinto(3)).to.equal("Lukion oppimäärä")
        expect(opinnot.getOppilaitos()).to.equal("Jyväskylän normaalikoulu")
      })
    })
    describe('Kaikki tiedot näkyvissä', function() {
      before(opinnot.expandAll)
      it('toimii', function() {
        expect(S('.lukionoppimaaransuoritus .osasuoritukset .tutkinnonosa:eq(0) .koulutusmoduuli .tunniste .value').text()).to.equal('Äidinkieli ja kirjallisuus')
      })
    })
    describe('Tulostettava todistus', function() {
      before(opinnot.avaaTodistus(3))
      it('näytetään', function() {
        // See more detailed content specification in LukioSpec.scala
        expect(todistus.vahvistus()).to.equal('Jyväskylä 4.6.2016 Reijo Reksi rehtori')
      })
    })
  })

  describe('Opintosuoritusote', function() {
    before(page.openPage, page.oppijaHaku.search('110496-9369', page.isOppijaSelected('Liisa')))
    before(opinnot.avaaOpintosuoritusote(1))

    describe('Kun klikataan linkkiä', function() {
      it('näytetään', function() {
      })
    })
  })

  describe('Lukioon valmistava koulutus', function() {
    before(page.openPage, page.oppijaHaku.search('300596-9615', page.isOppijaSelected('Luke')))
    describe('Oppijan suorituksissa', function() {
      it('näytetään', function() {
        expect(opinnot.getTutkinto()).to.equal("Maahanmuuttajien ja vieraskielisten lukiokoulutukseen valmistava koulutus")
        expect(opinnot.getOppilaitos()).to.equal("Jyväskylän normaalikoulu")
      })
    })
    describe('Kaikki tiedot näkyvissä', function() {
      before(opinnot.expandAll)
      it('toimii', function() {
        expect(S('.lukioonvalmistavankoulutuksensuoritus .osasuoritukset .koulutusmoduuli:eq(0) .nimi .value').text()).to.equal('Suomi toisena kielenä ja kirjallisuus')
      })
    })
    describe('Tulostettava todistus', function() {
      before(opinnot.avaaTodistus(0))
      it('näytetään', function() {
        // See more detailed content specification in LukioSpec.scala
        expect(todistus.vahvistus()).to.equal('Jyväskylä 4.6.2016 Reijo Reksi rehtori')
      })
    })
  })
})