import React from 'react'
import fecha from 'fecha'

export const Tiedonsiirtotaulukko = React.createClass({
  render() {
    const { rivit, showError } = this.props

    return (<div className="tiedonsiirto-taulukko">
      <table>
        <thead>
        <tr>
          <th className="tila">Tila</th>
          <th className="aika">Aika</th>
          <th className="hetu">Henkilötunnus</th>
          <th className="nimi">Nimi</th>
          <th className="oppilaitos">Oppilaitos</th>
          <th className="virhe">{showError ? 'Virhe' : 'Tiedot'}</th>
          <th className="tiedot"></th>
        </tr>
        </thead>
        {
          rivit.map((oppijaRivi, i) => <Lokiriviryhmä oppijaRivi={oppijaRivi} i={i} showError={showError} key={i}/>)
        }
      </table>
    </div>)
  }
})

const Lokiriviryhmä = React.createClass({
  render() {
    let { oppijaRivi, i, showError } = this.props

    const setExpanded = (expanded) => this.setState({expanded})
    const isExpanded = this.state && this.state.expanded
    const tiedonsiirtoRivit = oppijaRivi.rivit
    const isGroup = tiedonsiirtoRivit.length > 1
    return(<tbody>
      {
        tiedonsiirtoRivit.flatMap((rivi, j) => {
            const isParent = j == 0 && isGroup
            const isChild = j > 0 && isGroup
            const isHidden = isChild && !isExpanded
            return isHidden
              ? []
              : [<Lokirivi key={i + '-' + j} row={rivi} isParent={isParent} isChild={isChild} isExpanded={isExpanded} isEven={i % 2 == 1} showError={showError} setExpanded={setExpanded}/>]
          }
        )
      }
    </tbody>)
  }
})

const Lokirivi = React.createClass({
  render() {
    const extractName = (oppilaitokset) =>
      oppilaitokset && oppilaitokset.map((oppilaitos) => oppilaitos && oppilaitos.nimi && oppilaitos.nimi.fi).join(', ')

    const {row, isParent, isChild, isExpanded, isEven, showError, setExpanded} = this.props
    const dataToBeShown = this.state && this.state.dataToBeShown
    const showData = (data) => this.setState({dataToBeShown: data})
    const errorDetails = (virheet) => { return showError ?
      <div>
        <ul className="tiedonsiirto-errors">{
          virheet.map((virhe, i) => <li key={i}>{(virhe.key === 'badRequest.validation.jsonSchema') ? 'Viesti ei ole skeeman mukainen' : virhe.message}</li>)
        }</ul>
        <a className="virheen-tiedot" onClick={() => showErrors(virheet)}>virhe</a>
      </div> : <a className="virheen-tiedot" onClick={() => showErrors(virheet)}>virhe</a>}

    const dataLink = () => <a className="viestin-tiedot" onClick={() => showData(row.inputData)}>tiedot</a>

    const showErrors = (virheet) => showData(virheet)
    const nimi = row.oppija && ((row.oppija.kutsumanimi || '') + ' ' + (row.oppija.sukunimi || ''))
    const className = ((isParent || isChild) ? 'group ' : '') + (isEven ? 'even' : 'odd')

    return (<tr className={className}>
      <td className="tila">
        {
          isParent
            ? (isExpanded
              ? <a className="collapse" onClick={() => setExpanded(false)}>-</a>
              : <a className="expand" onClick={() => setExpanded(true)}>+</a>)
            : null
        }
        {
          row.virhe
            ? <span className="status fail">✕</span>
            : <span className="status ok">✓</span>
        }
      </td>
      <td className="aika">{fecha.format(fecha.parse(row.aika, 'YYYY-MM-DDThh:mm'), 'D.M.YYYY h:mm')}</td>
      <td className="hetu">{row.oppija && row.oppija.hetu}</td>
      <td className="nimi">{
        (row.oppija && row.oppija.oid)
          ? <a href={`/koski/oppija/${row.oppija.oid}`}>{nimi}</a> : nimi
      }</td>
      <td className="oppilaitos">{extractName(row.oppilaitos)}</td>
      <td className="virhe">{row.virhe && <span>{errorDetails(row.virhe)}</span>}</td>
      <td className="tiedot">
        {row.virhe && dataLink()}
        {
          dataToBeShown && <LokirivinData details={dataToBeShown} showData={showData}/>
        }
      </td>
    </tr>)
  }
})

const LokirivinData = React.createClass({
  render() {
    const { details, showData } = this.props
    console.log('showing details', details)
    return (<div className="lokirividata-popup">
      <a className="close" onClick={() => showData(false)}>Sulje</a>
      <pre>{JSON.stringify(details, null, 2)}</pre>
    </div>)
  }
})