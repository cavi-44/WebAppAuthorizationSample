import "./App.css";
import Post from "../components/Post.tsx";
import axios from "axios";

function App() {
  const posts: any = [
    { title: "dzieki dziala", description: "tutajk jest opis" },
  ];

  return (
    <div>
      essa
      {posts.map((post: any) => (
        <Post title={post.title} description={post.description} />
      ))}
      <button
        onClick={async () => {
          try {
            // 1. Słowo 'await' zatrzymuje kod i CZEKA na zakończenie rejestracji
            await axios.post("http://localhost:8080/api/auth/register", {
              login: "nowy_user",
              password: "tajne",
            });
            console.log("Zarejestrowano!");

            // 2. DOPIERO TERAZ wysyłamy logowanie
            const res = await axios.post(
              "http://localhost:8080/api/auth/login",
              {
                login: "nowy_user",
                password: "tajne",
              },
            );

            const token = res.data;
            localStorage.setItem("jwt_token", token);
            console.log("Zalogowano i zapisano token!");
          } catch (error) {
            console.error("Wystąpił błąd:", error);
          }
        }}
      >
        send
      </button>
    </div>
  );
}

export default App;
