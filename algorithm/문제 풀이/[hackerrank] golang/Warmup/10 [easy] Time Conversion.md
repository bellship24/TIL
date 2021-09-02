# Link

- [URL](https://www.hackerrank.com/challenges/time-conversion/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-03-00-50-01.png)

# Approach

문제 해석

- AM/PM 12시간 포맷을 24시간 포맷으로 변경하라
- AM 12시는 24시 포맷으로 0시, PM 12시는 24시 포맷으로 12시

로직

``` txt
if am
    if 12 시
        0시로 표현
    else
        그대로 표현
else
    if 12 시
        그대로 표현
    else
        +12
```

# Code

``` go
func timeConversion(s string) string {
    if strings.HasSuffix(s, "AM") && s[:2] == "12" {
        s = strings.Replace(s, "12", "00", 1)
    } else if strings.HasSuffix(s, "PM") && s[:2] != "12" {
        intHour, err := strconv.Atoi(s[:2])
        if err != nil {
            panic(timeConversion)
        }
        s = strconv.Itoa(intHour+12)+s[2:]
    }
    return s[:len(s)-2]
}
```

# Answer

``` go
func timeConversion(s string) string {
    if strings.HasSuffix(s, "AM") && s[:2] == "12" {
        s = strings.Replace(s, "12", "00", 1)
    } else if strings.HasSuffix(s, "PM") && s[:2] != "12" {
        change := fmt.Sprintf("%c%c", s[0]+1, s[1]+2)
        s = strings.Replace(s, s[:2], change, 1)
    }
    return s[:len(s)-2]
}
```

# Discussion

- slice indexing 에 따른 자료형 변화에 대한 이해가 부족해서 answer 보다 내가 작성한 code 가 더 와닿았다.

- conversion : 변환

- strings.HasSuffix : string 에서 suffix 유무 (c.f. strings.HasPrefix)

![](/.uploads/2021-08-03-01-18-51.png)

- strings.Replace : 주어진 string 에서 특정 문자열을 새로운 문자열로 대체 + 횟수 지정

![](/.uploads/2021-08-03-01-09-47.png)

- fmt.Sprintf : 포맷 식별자(format identifier) 에 따라 문자열 리턴. 주로 string 을 변형할 때 사용

![](/.uploads/2021-08-03-01-24-15.png)

- strconv.Atoi : string to int

![](/.uploads/2021-08-03-01-34-11.png)

- strconv.Itoa : Int to string

![](/.uploads/2021-08-03-01-40-50.png)

- fmt.Printf 로 변수의 자료형 출력하는 방법 : %T

``` go
s := "abcd"
mystring := string(s[len(s)-2])
fmt.Printf("%T, %s", mystring, mystring)

/* Output
string, c
*/
```