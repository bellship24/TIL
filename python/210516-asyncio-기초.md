# 210516-asyncio-기초

## 참고 URL

- [파이썬을 이용한 비트코인 자동매매 (개정판) / 웹소캣을 이용한 실시간 시세 처리 / 09-2 asyncio 기초](https://wikidocs.net/115650)

## 배경

### 웹소캣

웹소캣이란?

- 웹소캣은 컴퓨터 프로토콜로 클라이언트와 서버 간에 실시간 양방향 통신을 하는 기술이다.

웹소캣이 필요한 이유

- 기존에 REST API 를 사용하면 요청(request)-응답(response) 방식으로 지연이 생긴다.
- 반면에, 웹소캣 통신을 사용하면 서버로부터 실시간으로 데이터를 받아와 활용할 수 있다.

### python 에서의 웹소캣

websockets 과 asyncio

- python 에서 웹소캣을 사용하기 위해서는 여러 방법이 있는데 그 중에서 `websockets` 모듈을 사용하자.
- 이 모듈은 `asyncio` 라는 표준 모듈을 기반으로 개발되었으므로 먼저 asyncio 를 공부하자.

### asyncio - 동기 호출과 비동기 호출 방식

- 동기(synchronous) 호출 방식 : 어떤 요청에 대해 완전히 끝난 후 다음 요청을 처리하는 방식.
    - 코드 예제

        ``` python
        # ch09/09_01.py
        def sync_func1():
            print("Hello")
        
        def sync_func2():
            print("Bye")
        
        sync_func1()
        sync_func2()
        ```

    - 그림 예제
        ![](/images/2021-05-16-01-58-57.png)

- 비동기(asynchronous) 호출 방식 : 어떤 요청을 처리할 때 완전히 끝날 때 까지 기다리고 아무 일도 안하는 것이 아니라 적당한 스케줄링을 기반으로 대기 시간에 다른 요청을 처리하기도 하고 기존 요청에 대한 대기가 끝나면 다시 이어나가 처리하기도 하는 방식.
- 동기, 비동기 방식 차이
    ![](/images/2021-05-16-01-50-18.png)
- 비동기 방식이 동기 방식 보다 항상 좋은 것은 아니다. 데이터를 준비하는 데 시간이 걸리는 요청이라면 비동기식을 사용하고 데이터는 이미 준비되어 있으며 단순 연산으로 구성된 요청이라면 동기식을 사용하면 된다.

### 코루틴

python 에서 비동기 방식 함수를 정의하는 방법 - 코루틴

- python 에서 어떤 함수가 비동기 방식으로 처리되도록 설정하려면 함수 선언 `def` 앞에 `async` 라는 키워드를 붙여주면 된다.
- python 에서는 aync 키워드가 있는 함수를 코루틴(coroutine) 이라고 한다.

코루틴 함수를 호출하는 방법 - 이벤트 루프

- 코루틴 함수를 정의하는 방법, 호출하는 방법은 일반 함수와 방식이 다르다. 아래와 같이 코루틴 함수를 일반 함수 처럼 호출하면 에러가 발생한다.

    ``` python
    import asyncio

    async def async_func1():
        print("Hello")

    async_func1()
    # asyncio.run(async_func1())

    """
    # output
    async_func1 was never awaited        
    """
    ```

- 여러 코루틴을 잘 처리하기 위해서는 스케줄러가 필요한데 이를 `이벤트 루프` 라고 한다.
- 코루틴을 처리하기 전에 이벤트 루프를 만들고 코루틴의 처리가 끝난 후에 이벤트 루프를 닫아주면 된다. 이러한 역할을 간단히 처리해주는 것이 asyncio 모듈의 `run 함수` 이다. 아래 예제에서 asyncio 의 run 함수를 통해 정상 출력된 것을 확인할 수 있다.

    ``` python
    import asyncio

    async def async_func1():
        print("Hello")

    # async_func1()
    asyncio.run(async_func1())

    """
    # output
    Hello
    """

코루틴을 세부적으로 제어하는 방법

- asyncio.run 을 통해 코루틴을 실행할 수 있지만 때에 따라 이벤트 루프 동작을 세부 제어할 필요가 있을 수 있다. 이럴 때는 개발자가 직접 이벤트 루프를 얻고 이를 통해 코루틴을 처리한 후 이벤트 루프를 닫을 수 있다.

    ``` python
    import asyncio

    async def async_func1():
        print("Hello")

    loop = async.get_event_loop()
    loop.run_until_complete(async_func1())
    loop.close()
    ```

코루틴 예제

``` python
import asyncio

async def make_americano():
    print("Americano Start")
    await asyncio.sleep(3)
    print("Americano End")

async def make_latte():
    print("Latte Start")
    await asyncio.sleep(5)
    print("Latte End")

async def main():
    coro1 = make_americano()
    coro2 = make_latte()
    await asyncio.gather(
        coro1,
        coro2
    )

print("Main Start")
asyncio.run(main())
print("Main End")
"""
# Output
Main Start
Americano Start
Latte Start
Americano End
Latte End
Main End
"""
```

  - `asyncio.run(main())` 을 통해 코루틴의 이벤트 루프를 생성하고 메인 함수에 대한 코루틴을 실행한다. 메인 함수에는 `asyncio.gather()` 를 통해 coro1 과 coro2 코루틴 객체를 동시에 실행한다. 그래서 `main() -> coro1 -> coro1 에서 asyncio.sleep() -> coro2 -> coro1 완료 -> coro2 완료 -> 코루틴 종료` 순으로 실행됐다고 보면 된다.
  - await 을 통해 코루틴 안에서 다른 코루틴을 호출할 수 있다.

코루틴 값을 리턴 받는 예제

``` python
import asyncio

async def make_americano():
    print("Americano Start")
    await asyncio.sleep(3)
    print("Americano End")
    return "Americano"

async def make_latte():
    print("Latte Start")
    await asyncio.sleep(5)
    print("Latte End")
    return "Latte"

async def main():
    coro1 = make_americano()
    coro2 = make_latte()
    result = await asyncio.gather(
        coro1,
        coro2
    )
    print(result)

print("Main Start")
asyncio.run(main())
print("Main End")
"""
# Output
Main Start
Americano Start
Latte Start
Americano End
Latte End
['Americano', 'Latte']
Main End
"""
```

  - `make_americano()` 와 `make_latte()` 각각에 대해서 return 값을 추가했고 `main()` 함수에서 `asyncio.gather()` 를 `result` 변수에 담아 print 했다. 각 코루틴의 리턴값은 파이썬 리스트에 담겨서 전달된다.