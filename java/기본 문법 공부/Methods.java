/*
- 생성자 메서드(Methods)
- 메서드 오버로딩 : 인자에 따른 같은 이름 메서드들을 정의
- 가변 인자
*/

public class Methods {
    String name;

    // 생성자 메서드
    Methods() {
        name = "홍길동";
        System.out.printf("#생성자: %s\n", name);
    }

    // 인자가 없는 메서드
    void printName() {
        System.out.printf("#printName(): %s\n", name);
    }

    // 인자가 하나인 메서드(메서드 오버로딩), 인자 값이 출력됨.
    void printName(String name) {
        System.out.printf("#printName(String name): %s\n", name);
    }

    // 가변인자를 사용한 메서드
    void printName(String...name) {
        System.out.println("#printName(String...name)");
        for(String s : name) {
            System.out.println(s);
        }
    }

    // 인자가 두개인 메서드
    int calc(int num1, int num2) {
        return num1+num2;
    }

    public static void main(String[] args) {
        Methods m = new Methods();
        m.printName();
        m.printName("김길동");
        m.printName("아무개", "홍길동", "김사랑");
        System.out.printf("#calc(int num1, int num2): %d ", m.calc(20,50));
    }
}

/* Output
#생성자: 홍길동
#printName(): 홍길동
#printName(String name): 김길동
#printName(String...name)
아무개
홍길동
김사랑
#calc(int num1, int num2): 70
*/
