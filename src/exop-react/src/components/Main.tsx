import axios from "axios";

interface MyProps {
    url: string
}

export default function Main(props: MyProps) {
    console.log(props.url)
    return (<div>
            <h1>exop</h1>
            <p>visualize known exoplanets</p>
            <p>
                <button onClick={updateCatalog}>update catalog</button>
            </p>
            <p>
                <button onClick={createImage}>create image</button>
            </p>
            <div>
                <svg xmlns="http://www.w3.org/2000/svg" width="215.000mm" height="297.000mm">
                    <rect x="0.000mm" y="0.000mm" width="210.000mm" height="297.000mm"
                          style={{fill: 'white', opacity: 1.0}}/>
                    <line x1="21.000mm" y1="173.745mm" x2="199.500mm" y2="173.745mm" opacity="0.500"
                          style={{stroke: 'blue', strokeWidth: '12mm'}}/>
                    <line x1="21.000mm" y1="196.614mm" x2="199.500mm" y2="196.614mm" opacity="0.500"
                          style={{stroke: 'blue', strokeWidth: '0.045mm'}}/>
                    <circle cx="38.850mm" cy="196.614mm" r="22.869mm" opacity="0.500" fill="orange"/>
                    <circle cx="74.550mm" cy="196.614mm" r="22.869mm" opacity="0.500" fill="orange"/>
                    <circle cx="92.400mm" cy="196.614mm" r="11.435mm" opacity="0.500" fill="green"/>
                    <text x="21.000mm" y="82.269mm" fill="black" opacity="0.900" style={{
                        fontFamily: "serif",
                        fontSize: '20.790mm'
                    }}>Test Title
                    </text>
                    <text x="199.500mm" y="105.138mm" fill="black" opacity="0.900" fontFamily="serif"
                          fontSize="20.790mm" textAnchor="end">Test Title right
                    </text>
                    <text x="199.500mm" y="128.007mm" fill="black" opacity="0.900" fontFamily="serif"
                          fontSize="10.395mm" textAnchor="end">This is a normal text
                    </text>
                    <text x="21.000mm" y="150.876mm" fill="black" opacity="0.900" fontFamily="serif"
                          fontSize="10.395mm">This is a normal text
                    </text>
                </svg>

            </div>
        </div>
    );

    function updateCatalog() {
        console.log("update catalog image")
        axios.get(`${props.url}/image`)
            .then(response => {
                console.log(response);
                console.log(response.data);
            })
    }

    function createImage() {
        console.log("create image testsvg")
        axios.get(`${props.url}/testsvg`)
            .then(response => {
                console.log(response);
            })
    }

}

