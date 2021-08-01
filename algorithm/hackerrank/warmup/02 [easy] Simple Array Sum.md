
# Link

[URL](https://www.hackerrank.com/challenges/simple-array-sum/problem)

# Problem

![](/.uploads/2021-08-02-01-16-29.png)

# Approach

인풋 정의

- 첫번째 줄 input : array 사이즈 n
- 두번째 줄 input : array elements

절차

- array elements 를 합하여 담을 변수 sum 선언
- array elements 를 반복문하여 sum 에 더하기
- sum 을 return

# Code

``` go
func simpleArraySum(ar []int32) int32 {
    // Write your code here
    var sum int32 = 0
    for _, v := range ar {
        sum += v
    }
    return sum
}
```

# Answer

(동일)

# Disscusion

- array 자료형의 변수명을 ar 로 쓰곤 함
- 특정 array 의 각 요소에 대한 반복문 생성 방법

``` go
for key, value := range myAr {
    // Code here
}
```