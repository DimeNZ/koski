import React from 'react'
import Bacon from 'baconjs'
import Http from './http'
import {navigateToOppija, showError} from './router'
import {isValidHetu} from './hetu'
import {OpintoOikeus} from './CreateOpintoOikeus.jsx'

export const CreateOppija = React.createClass({
  render() {
    const opintoOikeus = this.state.opintoOikeus
    const {etunimet, sukunimi, kutsumanimi, hetu, inProgress} = this.state
    const validKutsumanimi = this.isKutsumanimiOneOfEtunimet(kutsumanimi, etunimet)
    const submitDisabled = !etunimet || !sukunimi || !kutsumanimi || !isValidHetu(hetu) || !validKutsumanimi || inProgress || !opintoOikeus.valid
    const buttonText = !inProgress ? 'Lisää henkilö' : 'Lisätään...'
    const hetuClassName = !hetu ? 'hetu' : isValidHetu(hetu) ? 'hetu' : 'hetu error'
    const kutsumanimiClassName = validKutsumanimi ? 'kutsumanimi' : 'kutsumanimi error'

    const errors = []

    if(!validKutsumanimi) {
      errors.push(<li key='2' className='kutsumanimi'>Kutsumanimen on oltava yksi etunimistä.</li>)
    }

    return (
      <form className='main-content oppija uusi-oppija' onInput={this.onInput}>
        <label className='etunimet'>
          Etunimet
          <input ref='etunimet'></input>
        </label>
        <label className={kutsumanimiClassName}>
          Kutsumanimi
          <input ref='kutsumanimi'></input>
        </label>
        <label className='sukunimi'>
          Sukunimi
          <input ref='sukunimi'></input>
        </label>
        <label className={hetuClassName}>
          Henkilötunnus
          <input ref='hetu'></input>
        </label>
        <hr/>
        <OpintoOikeus opintoOikeusBus={this.state.opintoOikeusBus}/>
        <button className='button blue' disabled={submitDisabled} onClick={this.submit}>{buttonText}</button>
        <ul className='error-messages'>
          {errors}
        </ul>
      </form>
    )
  },

  getInitialState() {
    return {etunimet: '', sukunimi: '', kutsumanimi: '', hetu: '', opintoOikeus: {valid: false}, opintoOikeusBus: Bacon.Bus()}
  },

  oppijaFromDom() {
    return {
      etunimet: this.refs.etunimet.value,
      sukunimi: this.refs.sukunimi.value,
      kutsumanimi: this.refs.kutsumanimi.value,
      hetu: this.refs.hetu.value.toUpperCase()
    }
  },

  componentDidMount() {
    this.state.opintoOikeusBus.onValue(o => {this.setState({opintoOikeus: o})})
    this.refs.etunimet.focus()
  },

  onInput() {
    this.setState(this.oppijaFromDom())
  },

  submit(e) {
    e.preventDefault()
    this.setState({inProgress: true})
    const createOppijaS = Http.post('/tor/api/oppija',  this.toCreateOppija()).map(oid => ({oid: oid}))
    createOppijaS.onValue(navigateToOppija)
    createOppijaS.onError((error) => {
      this.setState({inProgress: false})
      showError(error)
    })
  },

  toCreateOppija() {
    const {etunimet, sukunimi, kutsumanimi, hetu} = this.oppijaFromDom()
    const {tutkinto: {ePerusteDiaarinumero: peruste},oppilaitos: {organisaatioId: organisaatio}} = this.state.opintoOikeus
    return {
      etunimet: etunimet,
      sukunimi: sukunimi,
      kutsumanimi: kutsumanimi,
      hetu: hetu,
      opintoOikeus: {
        organisaatioId: organisaatio,
        ePerusteDiaarinumero: peruste
      }
    }
  },

  isKutsumanimiOneOfEtunimet(kutsumanimi, etunimet) {
    return kutsumanimi && etunimet ? etunimet.split(' ').indexOf(kutsumanimi) > -1 || etunimet.split('-').indexOf(kutsumanimi) > -1: true
  }
})