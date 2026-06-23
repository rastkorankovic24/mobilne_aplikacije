# Mobilne aplikacije - priprema za kolokvijum 2

## Projekti u ovom folderu

| Folder | Opis | Počni od |
|--------|------|----------|
| **Kolokvijum2** | Originalni pripremni (radi) | Glavna baza za ispit |
| **Kolokvijum2A** | Proizvodi + EditText pretraga, 12 u bazi | Vežba API + filter |
| **Kolokvijum2B** | Korisnici + CheckBox, briše poslednji | Vežba uslova + drugačije brisanje |
| **Kolokvijum2C** | Komentari + brojač (Broj: N) | Vežba dodatnog TextView-a |
| **Kolokvijum2D** | Najbliži originalu (5 postova, long click) | Prva probna varijanta |

Svaki projekat ima fajl **ZADATAK.txt** sa opisom zadatka na srpskom.

## Kako otvoriti

Android Studio → File → Open → izaberi folder (npr. Kolokvijum2A) → Sync → Run

## Sablon za ispit

`Kolokvijum2/KOLOKVIJUM_SABLON.txt` — kopiraj delove koda kad menjaš projekat.

## API (beeceptor)

- Postovi: `https://dummy-json.mock.beeceptor.com/posts`
- Korisnici: `https://dummy-json.mock.beeceptor.com/users`
- Komentari: `https://dummy-json.mock.beeceptor.com/comments`

*Napomena: Varijanta A koristi endpoint `posts` jer beeceptor nema `/products` — polja se mapiraju u model Product.*
