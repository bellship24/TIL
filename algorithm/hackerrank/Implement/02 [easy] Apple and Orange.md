# Link

- [URL](https://www.hackerrank.com/challenges/apple-and-orange/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-03-09-04-09.png)
![](/.uploads/2021-08-03-09-04-19.png)
![](/.uploads/2021-08-03-09-04-26.png)

# Approach

문제 해석

- d 가 양수이면 나무 기준 오른쪽으로, 음수이면 나무 기준 왼쪽으로 떨어짐
- s~t 사이에 떨어진 m 사과와 n 오렌지의 개수 출력

로직

``` txt
apple_cnt, orange_cnt := 0, 0
    for _, v := range apples {
    if s <= apples + a && apples + a <= t {
        apple_cnt++
    }
}

for _, v := range oranges {
    if s <= oranges + b && oranges + b <= t {
        orange_cnt++
    }
}

fmt.Printf("%d\n%d", apple_cnt, orange_cnt)
```

# Code

``` go
func countApplesAndOranges(s int32, t int32, a int32, b int32, apples []int32, oranges []int32) {
    apple_cnt, orange_cnt := 0, 0
    
    for _, v := range apples {
        if s <= v + a && v + a <= t {
            apple_cnt++
        }
    }
    
    for _, v := range oranges {
        if s <= v + b && v + b <= t {
            orange_cnt++
        }
    }

    fmt.Printf("%d\n%d", apple_cnt, orange_cnt)
}
```

# Answer

c.f. 함수 만들어 사용하기

``` go
func count(s, t, a int32, l []int32) int32 {
    var c int32
    for _, v := range l {
        if (s <= v + a) && (v + a <= t) {
            c++
        }
    }
    return c
}

func countApplesAndOranges(s int32, t int32, a int32, b int32, apples []int32, oranges []int32) {
    fmt.Printf("%d\n%d", count(s, t, a, apples), count(s, t, b, oranges))
}
```

# Discussion

- 사과와 오랜지에 대한 연산 산출물을 구할 때, 로직은 같다. 다만, array 의 길이에 따라 반복문의 횟수 차이만 있다. 이 경우, 두 반복문을 합치는 방법으로 **함수 생성**을 사용할 수 있다. 공통 부분은 그대로 쓰고 차이있는 부분만 변수화 해서 사용하면 될듯 하다.