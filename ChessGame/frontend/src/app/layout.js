import "./globals.css";

export const metadata = {
  title: "Checkmate | Online Chess",
  description: "Play a two-player chess game with Elo-based matchmaking.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
