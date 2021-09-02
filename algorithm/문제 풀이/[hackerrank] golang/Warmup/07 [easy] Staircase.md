# Link

- [URL](https://www.hackerrank.com/challenges/staircase/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-02-15-59-27.png)

# Approach

문제 해석

- 주어진 n 만큼 # 문자로 계단 만들기

예제

- n 이 4 일 때, 첫번 째 출력은 "공백, 공백, 공백, #" 이다. 두번 째 출력은 "공백, 공백, #, #" 이다.

로직

``` txt
n 번 반복문
    row 당 n - row 만큼의 공백 출력
    row 당 row 만큼의 # 출력
```

# Code

``` go
func staircase(n int32) {
    for i := int32(0);
}

```

# Answer

``` go
func staircase(n int32) {
    for i := int32(0); i < n; i++ {
        fmt.Printf("%s%s\n",
          strings.Repeat(" ", int(n-i-1)),
          strings.Repeat("#", int(i+1)))
    }
}
```

# Discussion

- staircase : 계단
- any spaces : 공백
- `strings.Repeat()` : 특정 문자를 반복해서 print 할 때 사용하자.

``` go
func strings.Repeat(s string, count int) string
```

- `fmt.Printf("%4s", ...)` : 특정 문자열 출력 시에 문자열 사이즈를 정하고 빈 공간은 공백으로 생성하고 싶다면, 포멧팅을 다음과 같이 사용하자. `fmt.Printf("%6s", "d")`. 공백은 앞에서부터 입력되는 데, 역순으로 하고 싶으면 다음과 같이 `-` 를 입력하자. `fmt.Printf("%-6s", "d")`