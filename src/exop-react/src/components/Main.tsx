import axios from "axios";
import {useState} from "react";
import img01 from "./img01a.png";
import img02 from "./img02.png";

interface MyProps {
    url: string
}

interface MainState {
    info: string
    showImageList: boolean
}

export default function Main(props: MyProps) {
    const [state, setState] = useState<MainState>({info: "", showImageList: false});
    const openInNewTab = (url: string) => {
        const newWindow = window.open(url, '_blank', 'noopener,noreferrer')
        if (newWindow) newWindow.opener = null
    }
    return (<div>
            <div>
                <button onClick={showCreateImage}>create poster</button>
                &nbsp;&nbsp;&nbsp;&nbsp;
                <button onClick={updateCatalog}>update catalogue</button>
                &nbsp;&nbsp;&nbsp;&nbsp;
                <button onClick={help}>?</button>
            </div>
            <p className="info-text" dangerouslySetInnerHTML={{__html: state.info}}></p>
            <div className="info-text" style={{display: state.showImageList ? 'block' : 'none'}}>
                <table className="info-table-top">
                    <tbody>
                    <tr>
                        <td>Click one of the posters sizes to create the poster.</td>
                    </tr>
                    </tbody>
                </table>
                <table>
                    <tbody>
                    <tr>
                        <td><img className={"list-img"} alt={"img01"} src={img01}></img></td>
                        <td>Earth-like Distance<br/>
                            <button className="info-text" onClick={() => createImage("img01", "A4")}>A4</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img01", "A3")}>A3</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img01", "A2")}>A2</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img01", "A1")}>A1</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img01", "A0")}>A0</button>
                            &nbsp;&nbsp;
                        </td>
                        <td>Planetary systems containing one planet that has about the same <br/>
                            distance to its star as the earth to the sun
                        </td>
                    </tr>
                    <tr>
                        <td><img className={"list-img"} alt={"img02"} src={img02}></img></td>
                        <td>Planet sizes<br/>
                            <button className="info-text" onClick={() => createImage("img02", "A4")}>A4</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img02", "A3")}>A3</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img02", "A2")}>A2</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img02", "A1")}>A1</button>
                            &nbsp;&nbsp;
                            <button className="info-text" onClick={() => createImage("img02", "A0")}>A0</button>
                            &nbsp;&nbsp;
                        </td>
                        <td>Planetary systems containing at least one planet with known size.<br/>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <table className="info-table-bottom">
                    <tbody>
                    <tr>
                        <td>Poster sizes</td>
                        <td>
                            A4&nbsp;&nbsp;210 x 297mm, 8.3' x 11.7'<br/>
                            A3&nbsp;&nbsp;297 x 420mm, 11.7' x 16.5'<br/>
                            A2&nbsp;&nbsp;420 x 594mm, 16.6' x 23.4'<br/>
                            A1&nbsp;&nbsp;594 x 841mm, 23.4' x 33.1'<br/>
                            A0&nbsp;&nbsp;841 x 1189mm, 33.1' x 46.8'<br/>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    );

    function updateCatalog() {
        console.log("update catalogue")
        setState({info: "update in progress ...", showImageList: false})
        let url = `${props.url}/update`
        axios.get(`${props.url}/update`)
            .then(response => {
                console.log(response.data);
                setState({info: response.data, showImageList: false})
            })
            .catch(reason => {
                let msg = `Error calling url ${url}. ${reason.message}`
                console.log(JSON.stringify(reason));
                setState({info: msg, showImageList: false})
            })
    }

    function createImage(name: string, size: string) {
        console.log("create image")
        openInNewTab(`${props.url}/image?name=${name}&size=${size}`)
    }

    function showCreateImage() {
        console.log("sho create image")
        if (state.showImageList) setState({info: "", showImageList: false})
        else setState({info: "", showImageList: true})
    }

    function help() {
        console.log("help")
        let info = "<table><tbody>\n" +
            "    <tr><td>create poster:</td><td>creates a poster of the given size in your browser " +
            "which you <br>can print or export as pdf.</td></tr>\n" +
            "    <tr><td>update catalogue:</td><td>Loads the latest version of the exoplanet catalogue on the server. <br>" +
            "        If the update was successful a list of the latest updates is displayed in <br>the output field.</td></tr>\n" +
            "</tbody></table>"
        if (state.info.length === 0) setState({info: info, showImageList: false})
        else setState({info: "", showImageList: false})
    }


}

