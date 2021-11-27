import java.net.*;
import java.text.SimpleDateFormat;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

public class ChatThread extends Thread {
   ChatServer myServer; // ChatServer 객체
   Socket mySocket; // 클라이언트 소켓

   PrintWriter out; // 입출력 스트림
   BufferedReader in;
   int b = 0, bc = 0;

   public ChatThread(ChatServer server, Socket socket) // 생성자
   {
      super("ChatThread");

      myServer = server;
      mySocket = socket;

      out = null;
      in = null;
   }

   public void sendMessage(String msg) throws IOException // 메시지를 전송
   {

      out.println(msg);
      out.flush();
   }

   public void disconnect() // 연결을 종료
   {
      try {
         out.flush();
         in.close();
         out.close();
         mySocket.close();
         myServer.removeClient(this);
      } catch (IOException e) {
         System.out.println(e.toString());
      }
   }

   public void run() // 쓰레드 시작
   {
      try {
         // 소켓을 이용하여 상대방과 입출력 스트림을 생성
         out = new PrintWriter(new OutputStreamWriter(mySocket.getOutputStream()));
         in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
         while (true) { // 클라이언트 보낸 메시지를 처리하기 위해 기다림
            String inLine = in.readLine();
            // 기다리고 있다가 클라이언트가 보낸 메시지가 있는 경우 읽어들임
            if (!inLine.equals("") && !inLine.equals(null)) {
               messageProcess(inLine); // 클라이언트가 보낸 메시지를 확인하여 현재 접속한 모든 클라이언트에게 브로드캐스트
            }
         }
      } catch (Exception e) {
         disconnect();
      }
   }

   // 클라이언트가 보낸 메시지를 확인한 후 처리
   public void messageProcess(String msg) throws IOException {

      System.out.println(msg); // 화면에 출력
      StringTokenizer st = new StringTokenizer(msg, "|");// 규칙에 따라 받은 메시지를 분리하여 확인
      String command = st.nextToken(); // 메시지의 명령 command 부분
      String talk = st.nextToken();
      // 메시지의 대화 talk 부분

      if (command.equals("LOGIN")) { // 받은 메시지가 LOGIN 이면 처음 접속 메시지이기 때문에 접속 처리
         System.out.println("[접속] " + mySocket);
         try { // 새로운 클라이언트가 접속하여 추가된 클라이언트 수를 브로드캐스트
            myServer.broadcast("~~"+talk.substring(0, talk.indexOf("S"))/* 2 */ + "님이 접속하였습니다~~");// 접속한 이름을 출력해준다
            myServer.broadcast("[현재 접속자수] " + myServer.clientNum + "명");
            myServer.HT(talk.substring(0, talk.indexOf("S")),
                  (ChatThread) myServer.clientVector.elementAt(myServer.clientNum - 1));// 3
            // 접속한 클라이언트의 정보를 이름(키),클라이언트정보(벨류)값으로 헤쉬테이블에 저장
         } catch (IOException e) {
            System.out.println(e.toString());
         }
      } else if (command.equals("change")) { // 변경이라는 명령어가 내려오면 처리

         try {
            myServer.broadcast(talk);// 변경 상황을 출력해줌

            myServer.HTC(talk.substring(talk.indexOf(">") + 3, talk.indexOf("(") - 2),
                  (ChatThread) myServer.ht.get(talk.substring(2, talk.indexOf("]"))),
                  talk.substring(2, talk.indexOf("]")));// 3
            // 변경후 이름으로 다시 헤쉬테이블을 수정해줌(수정후 이름을 다시 키값으로 가져가고 수정전 이름이로 벨류값을 불러와 다시 헤쉬테이블에 저장
            // a.substring( a.indexOf(">")+3, a.indexOf("(")-1);후 이름
            // a.substring(1, a.indexOf("]")전 이름
            // System.out.println(b.substring(2, b.indexOf("]")));
            // System.out.println(b.substring(b.indexOf(">") + 3, b.indexOf("(")-2));
         } catch (IOException e) {
            System.out.println(e.toString());
         }
      } else if (command.charAt(0) == 'W') { // W라는 명령어가 들어오면 귓속말 수행

         try {
            myServer.unicast(talk, command.substring(command.indexOf(">") + 1),talk.substring(1, talk.indexOf("]")));// 입력된 채팅과 명령어 부분에서 귓속말 보낼
            // 사람(command.substring(command.indexOf(">")+1))의
            // 이름을 따와서 그사람에게 보내줌
         } catch (IOException e) {
            System.out.println(e.toString());
         }
      } else if (command.equals("LOGOUT")) { // 받은 메시지가 LOGOUT 이면 종료 메시지이므로
         try { // 제거된 클라이언트의 수를 브로드캐스트
            myServer.clientNum--;
            myServer.broadcast("<<" + talk.substring(0, talk.indexOf("S"))/* 2 */ + "님이 퇴장하셨습니다.>>");// 접속한 이름을
                                                                                 // 출력해준다
            myServer.HTR(talk.substring(0, talk.indexOf("S")));
            myServer.broadcast("[현재 접속자수] " + myServer.clientNum + "명");
         } catch (IOException e) {
            System.out.println(e.toString());
         }
         disconnect(); // 연결 종료
      }
      else if (command.equals("LOGOUT1")) { // 받은 메시지가 LOGOUT 이면 종료 메시지이므로
         try { // 제거된 클라이언트의 수를 브로드캐스트
            myServer.clientNum--;
            myServer.broadcast("<<" + talk.substring(0, talk.indexOf("S"))/* 2 */ + "님이 강제 퇴장당하셨습니다.>>");// 접속한 이름을
            myServer.HTR(talk.substring(0, talk.indexOf("S")));                                                                     // 출력해준다
            myServer.broadcast("[현재 접속자수] " + myServer.clientNum + "명");
         } catch (IOException e) {
            System.out.println(e.toString());
         }
         disconnect(); // 연결 종료
      }
      /**********************************************************************************/
      else if (command.equals("봇")) {
         if (talk.contains("로또")) {
            // 45개의 공을 만든다
            ArrayList<Integer> numbers = new ArrayList<Integer>();
            for (int i = 1; i <= 45; i++) {
               numbers.add(i); // 1~45
            }
            // 섞는다
            Collections.shuffle(numbers);

            // 뽑는다
            int[] picked = new int[6];
            for (int i = 0; i < 6; i++) { // numbers의 앞 6개 숫자를 가져옴
               picked[i] = numbers.get(i);
            }

            // 결과출력
            try {
               myServer.Bunicast(talk + "\n<  오늘의 로또 추천번호  >\n" + Arrays.toString(picked),talk.substring(1, talk.indexOf("]")));
            } catch (IOException e) {
               System.out.println(e.toString());
            }
         } else if (talk.contains("메뉴")) {
            int a;
            String food[] = { "치킨", "피자", "중화요리", "삼겹살", "곱창", "라면", "떡볶이", "햄버거", "족발", "보쌈", "돈까스", "제육볶음", "냉면",
                  "부대찌개", "닭발", "국밥" };
            Random rm = new Random();
            a = rm.nextInt(food.length);
            try {
               myServer.Bunicast(talk + "\n<  오늘의 추천메뉴는 : " + food[a] + "입니다.  >",talk.substring(1, talk.indexOf("]")));
            } catch (IOException e) {
               System.out.println(e.toString());
            }

         } else if (talk.contains("고마워") || talk.contains("땡큐")) {
            try {
               myServer.Bunicast(talk + "\n[BOT] : 별 말씀을요(^-^)",talk.substring(1, talk.indexOf("]")));
            } catch (IOException e) {
               System.out.println(e.toString());
            }

         } else if (talk.contains("안녕") || talk.contains("하이") || talk.contains("반가워")) {
            try {
               myServer.Bunicast(talk + "\n[BOT] : 안녕하세요!",talk.substring(1, talk.indexOf("]")));
               try {
                  File theFile = new File("\\\\Mac\\Home\\Desktop\\봇 음성파일\\jammin_hi.wav");
                  FileInputStream fis = new FileInputStream(theFile);
                  AudioStream as = new AudioStream(fis);
                  AudioPlayer.player.start(as);
               } catch (Exception ex) {
                  System.out.println(ex);
               }
            } catch (IOException e) {
               System.out.println(e.toString());
            }
         } else if (talk.contains("노래")) {
            myServer.Bunicast(talk + "\n[BOT] : 한곡 뽑아보겠습니다.!",talk.substring(1, talk.indexOf("]")));
            try {
               File theFile = new File("\\\\Mac\\Home\\Desktop\\봇 음성파일\\song.wav");
               FileInputStream fis = new FileInputStream(theFile);
               AudioStream as = new AudioStream(fis);
               AudioPlayer.player.start(as);
            } catch (Exception ex) {
               System.out.println(ex);
            }
         } else {
            try {
               myServer.Bunicast(talk + "\n[BOT] : 명령을 입력해주세요",talk.substring(1, talk.indexOf("]")));
            } catch (IOException e) {
               System.out.println(e.toString());
            }
         }

      }
      /**********************************************************************************/

      else if (command.equals("접속자")) { // 명령어로 접속자수를 애플릿창에 내보내줌
         try {

            myServer.broadcast("--"+myServer.ht.keySet() + "님이 현재 접속중 입니다.--");

         } catch (IOException e) {
            System.out.println(e.toString());
         }
      } else if (command.equals("TALK")) {
         try { // LOGIN, LOGOUT 이외의 경우는 일반 메시지로 모든 클라이언트에게 받은 메시지 전송
            myServer.broadcast(talk);
         } catch (IOException e) {
            System.out.println(e.toString());
         }

         try {
            File theFile = new File("\\\\Mac\\Home\\Desktop\\봇 음성파일\\sound.wav");
            FileInputStream fis = new FileInputStream(theFile);
            AudioStream as = new AudioStream(fis);
            AudioPlayer.player.start(as);
         } catch (Exception ex) {
            System.out.println(ex);
         }
      } else {
         try { // LOGIN, LOGOUT 이외의 경우는 일반 메시지로 모든 클라이언트에게 받은 메시지 전송
            myServer.broadcast(talk);
         } catch (IOException e) {
            System.out.println(e.toString());
         }
      }
   }
}
